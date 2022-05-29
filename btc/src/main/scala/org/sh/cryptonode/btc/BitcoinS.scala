package org.sh.cryptonode.btc

import org.sh.cryptonode.btc.BitcoinUtil._
import org.sh.cryptonode.btc.DataStructures.{TxIn, TxOut}

import scala.util.Try

object BitcoinS {

  var isMainNet = true // set to false for testnet

  def isBECH32_Address(address: String) = {
    getAddressType(getScriptPubKeyFromAddress(address)).getOrElse(
      throw new Exception(s"Invalid address $address")
    ) == BECH32_ADDRESS_TYPE
  }
  def isP2SH_Address(address: String) = {
    getAddressType(getScriptPubKeyFromAddress(address)).getOrElse(
      throw new Exception(s"Invalid address $address")
    ) == P2SH_ADDRESS_TYPE
  }
  def isP2PKH_Address(address: String) = {
    getAddressType(getScriptPubKeyFromAddress(address)).getOrElse(
      throw new Exception(s"Invalid address $address")
    ) == P2PKH_ADDRESS_TYPE
  }
  def isValidAddress(address: String) =
    Try(getAddressType(getScriptPubKeyFromAddress(address)))
      .getOrElse(None)
      .isDefined

  def isMainNetAddress(address: String): Boolean =
    Try {
      val (spk, isMainNetAddress) = getScriptPubKeyAndNetFromAddress(address)
      getAddressType(spk).nonEmpty && isMainNetAddress
    }.getOrElse(false)

  // wrapper over "Advanced", that uses default version, locktime and sets witnesses to empty
  def createTxRaw(ins: Seq[TxIn], outs: Seq[TxOut]) =
    createNonSegWitTx(defaultTxVersion, ins, outs, defaultTxLockTime)

  def signTx(unsignedTx: Array[Byte], inKeysAmts: Seq[(PrvKey, BigInt)]) =
    inKeysAmts.zipWithIndex.foldLeft(unsignedTx) {
      case (prevTx, ((key, amt), i)) => key.signTx(prevTx, i, amt)
    }

}
