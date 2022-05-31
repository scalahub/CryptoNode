package org.sh.cryptonode

import org.sh.cryptonode.btc.{Bitcoin, BitcoinUtil}
import org.sh.cryptonode.util.Bech32.{decode, encode, str2IntMap}
import org.sh.cryptonode.util.BytesUtil.ByteArrayToBetterByteArray

import scala.util.Try

object TestBech32 extends App {
  // test encoding / decoding

  val testVectors1 = Seq( // bech32 -> (hrp, data)
    "bc1rhwez7me9h"                                                                              -> ("bc", "rhwe"),
    "A12UEL5L"                                                                                   -> ("a", ""),
    "an83characterlonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1tt5tgs" -> ("an83characterlonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio", ""),
    "abcdef1qpzry9x8gf2tvdw0s3jn54khce6mua7lmqqqxw"                                              -> ("abcdef", "qpzry9x8gf2tvdw0s3jn54khce6mua7l"),
    "11qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqc8247j" -> ("1", "qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq"),
    "split1checkupstagehandshakeupstreamerranterredcaperred2y9e3w"                               -> ("split", "checkupstagehandshakeupstreamerranterredcaperred")
  )

  val testVectors2 = Seq( // bech32 -> (scriptPubKey, isMainNet)
    "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4"                                 -> ("0014751e76e8199196d454941c45d1b3a323f1433bd6", true),
    "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7"             -> ("00201863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262", false),
    "bc1pw508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7k7grplx" -> ("5128751e76e8199196d454941c45d1b3a323f1433bd6751e76e8199196d454941c45d1b3a323f1433bd6", true),
    "BC1SW50QA3JX3S"                                                             -> ("6002751e", true),
    "bc1zw508d6qejxtdg4y5r3zarvaryvg6kdaj"                                       -> ("5210751e76e8199196d454941c45d1b3a323", true),
    "tb1qqqqqp399et2xygdj5xreqhjjvcmzhxw4aywxecjdzew6hylgvsesrxh6hy"             -> ("0020000000c4a5cad46221b2a187905e5266362b99d5e91c6ce24d165dab93e86433", false)
  )

  val testVectors3 = Seq( // invalid bech32 addresses
    "tc1qw508d6qejxtdg4y5r3zarvary0c5xw7kg3g4ty"                                   -> "Invalid human-readable part",
    "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5"                                   -> "Invalid checksum",
    "BC13W508D6QEJXTDG4Y5R3ZARVARY0C5XW7KN40WF2"                                   -> "Invalid witness version",
    "bc1rw5uspcuh"                                                                 -> "Invalid program length",
    "bc10w508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7kw5rljs90" -> "nvalid program length",
    "BC1QR508D6QEJXTDG4Y5R3ZARVARYV98GJ9P"                                         -> "Invalid program length for witness version 0 (per BIP141)",
    "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sL5k7"               -> "Mixed case",
    "bc1zw508d6qejxtdg4y5r3zarvaryvqyzf3du"                                        -> "zero padding of more than 4 bits",
    "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3pjxtptv"               -> "Non-zero padding in 8-to-5 conversion",
    "bc1gmk9yu"                                                                    -> "Empty data section"
  )

  def bech32EncodingTest(bech32: String, hrp: String, data: String) = {
    val decoded = decode(bech32)
    require(
      (hrp, data.map(str2IntMap)) == decoded,
      s"expected ($hrp, $data) != actual decoded $decoded"
    )
    val encoded: String = encode(hrp, data.map(str2IntMap))
    require(
      bech32.equalsIgnoreCase(encoded),
      s"expected $bech32 != actual encoded $encoded"
    )
  }

  def bech32ScriptPubKeyTest(bech32: String, scriptPubKeyExpected: String) = {
    val (scriptPubKey, _) =
      BitcoinUtil.getScriptPubKeyAndNetFromAddressBech32(bech32)
    val scriptPubKeyHex = scriptPubKey.toArray.encodeHex
    require(
      scriptPubKeyHex.equalsIgnoreCase(scriptPubKeyExpected),
      s"expected $scriptPubKeyExpected != actual $scriptPubKeyHex"
    )
  }

  testVectors1.foreach {
    case (bech32, (hrp, data)) => bech32EncodingTest(bech32, hrp, data)
  }

  testVectors2.foreach {
    case (bech32, (scriptPubKey, mainNet)) =>
      Bitcoin.isMainNet = mainNet
      bech32ScriptPubKeyTest(bech32, scriptPubKey)

      assert(
        Bitcoin.isValidAddress(bech32),
        s"$bech32 should have passed test2"
      )
  }

  testVectors3.foreach {
    case (bech32, reason) =>
      assert(
        Try(
          BitcoinUtil.getScriptPubKeyAndNetFromAddressBech32(bech32)
        ).isFailure,
        s"$bech32 should have failed because: $reason"
      )
      assert(
        !Bitcoin.isValidAddress(bech32),
        s"$bech32 should have failed test 2 because +$reason"
      )
  }
  println("Bech32 tests passed")
}
