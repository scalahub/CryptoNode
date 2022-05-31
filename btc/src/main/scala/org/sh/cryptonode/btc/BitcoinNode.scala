package org.sh.cryptonode.btc

import org.sh.cryptonode.btc.NetworkSeeds.{mainNetSeeds, testNetSeeds}
import org.sh.cryptonode.net.Node
import org.sh.cryptonode.util.StringUtil._

class BitcoinNode(isMainNet: Boolean) extends Node {
  lazy val id                = "Bitcoin"
  lazy val version: Int      = 70003
  lazy val userAgent: String = "/BitcoinS:0.1/"
  lazy val serviceBit: Int   = 0
  lazy val magicBytes =
    if (isMainNet) "F9BEB4D9".decodeHex else "0B110907".decodeHex

  val seeds =
    if (isMainNet)
      Seq(
        "seed.bitcoin.sipa.be:8333",
        "dnsseed.bluematt.me:8333",
        "dnsseed.bitcoin.dashjr.org:8333",
        "seed.bitcoinstats.com:8333",
        "seed.bitcoin.jonasschnelli.ch:8333",
        "seed.btc.petertodd.org:8333"
      ) ++ mainNetSeeds.map { case (addr, port) => addr + ":" + port }
    else
      Seq(
        "testnet-seed.bitcoin.jonasschnelli.ch:18333",
        "seed.tbtc.petertodd.org:18333",
        "testnet-seed.bluematt.me:18333",
        "testnet-seed.bitcoin.schildbach.de:18333"
      ) ++ testNetSeeds.map { case (addr, port) => addr + ":" + port }

}
