package org.sh.cryptonode.net

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.Tcp._
import akka.util.ByteString
import org.sh.cryptonode.btc.{BlockParser, _}
import org.sh.cryptonode.net.DataStructures._
import org.sh.cryptonode.net.Parsers._

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

object Peer {
  val system = ActorSystem("BTCPeerSystem")
  var debug  = false // default to false

  def props(peerGroup: ActorRef, config: PeerConfig) =
    Props(classOf[Peer], peerGroup, config)

  val actCtr = new AtomicInteger(0)
  def connectTo(hostName: String, peerGroup: ActorRef, config: PeerConfig) = {
    val arr    = hostName.split(":")
    val host   = arr.dropRight(1).mkString(":")
    val port   = arr.last.toInt
    val ctr    = actCtr.getAndIncrement
    val peer   = system.actorOf(props(peerGroup, config), name = s"peer_$hostName$ctr")
    val remote = system.actorOf(P2PClient.props(new InetSocketAddress(host, port), peer, config.magicBytes), s"peer-client_$hostName$ctr")
    peer ! remote // Send remote address to peer. It will store for internal use
    peer          // return an actor ref to the peer
  }
}

case class PeerConfig(relay: Boolean, version: Int, userAgent: String, serviceBit: Int, magicBytes: Array[Byte])

class Peer(peerGroup: ActorRef, config: PeerConfig) extends Actor {

  var optClientActor: Option[ActorRef] =
    None // reference to the actor that talks to remote peer
  var optRemoteAddr: Option[InetSocketAddress] = None // address of remote peer
  var optLocalAddr: Option[InetSocketAddress] =
    None // our local address, needed for ADDR messages

  def peer = optRemoteAddr.map(_.toString).getOrElse("none") // string representation of remote peer for debug display

  val dataProcessor = new DataProcessor(config.magicBytes)
  def receive = {
    case data: ByteString => // data packet received from remote peer (INV/Tx/Block/Version/etc)
      dataProcessor.getCommands(data).foreach(processCommand) // process the data (parse it and obtain the commands in the data)

    case a: ActorRef => optClientActor = Some(a) // received actorRef of remote.

    case m: P2PMsg => optClientActor.map(_ ! m) // push tx, get tx/block etc (commands coming fromg PeerGroup)

    case Connected(remote, local) => // connect message send initially (or during reconnect, once implemented)
      optRemoteAddr = Some(remote)
      optLocalAddr = Some(local)
      sender ! VersionMsg(config.version, config.userAgent, config.serviceBit, local, remote, config.relay) // send version message
      peerGroup ! ("connected", remote.getHostString)
    case "connection closed" => // remote connection closed
      println(s"Remote connection closed: $peer")
      peerGroup ! "disconnected"
      context.stop(self)

    case "stop" => // someone sent this actor a shutdown signal
      println(s"Peer received stop signal: $peer")
      optClientActor.map(_ ! "close") // ask client to disconnect

    case any => println(s"Peer got unknown message: [$any] from [$sender]")
  }

  def processCommand(commandBytes: (String, ByteString)): Unit = {
    val (command, byteString) = commandBytes
    val bytes                 = byteString.toArray
    command match {
      case `getDataCmd` | `notFoundCmd` => peerGroup ! (command, new InvPayloadParser(bytes).inv)
      case `getAddrCmd` | `verAckCmd`   => peerGroup ! command
      case `rejectCmd`                  => peerGroup ! new RejectPayloadParser(bytes).rej
      case `invCmd`                     => peerGroup ! new InvPayloadParser(bytes).inv
      case `addrCmd`                    => peerGroup ! new AddrPayloadParser(bytes).addr
      case `versionCmd` =>
        val otherVersion = new VersionPayloadParser(bytes).version.version.int32
        if (otherVersion >= config.version) sender ! VerAckMsg
        else println(s"[!!] Received version $otherVersion < config version ${config.version}")
      case `pingCmd`  => sender ! PongMsg(new PingPayloadParser(bytes).ping)
      case `txCmd`    => peerGroup ! new TxParser(bytes.toArray).getTx
      case `blockCmd` => peerGroup ! new BlockParser(bytes.toArray).getBlock
      case `alertCmd` =>                                    // ignore
      case cmd        => println(s"Unhandled command $cmd") // do nothing for now
    }
  }
}
