package org.sh.cryptonode

// Wrapper for tests that can be done stand-alone, that is, without requiring bitcoind running
object RunStandAloneTests {
  val objects = Seq(
    TestAddress, // address validation
    TxParserTest, // various tests for Tx parsing. Test vectors taken from Internet
    TestTx, // Tx creation, signing and verification
    TestSegWitSample, // SegWit sample from spec doc. Test that it is parsed, signed correctly as per the test vectors given there
    TestRFC6979, // test that deterministic signature generation works correct. Test vectors from various sources (mentioned in object's code)
    Validate_RFC6979_TestVectors, // more RFC6979 deterministic test vectors. Validated on chat (IRC)
    TestKeyRecovery, // test if key recovery correctly works (key recovery is used in Bitcoind "sign message" option, not for tx signing
    TestECC, // test various ECC operations
    TestCompressedPubKey, // tests if compressed pub keys decode and sign correctly. Test vectors from StackOverflow and IRC
    TestBlockParser, // tests that block is correctly being parsed. Test vectors from varous sources
    TestBase64,
    TestBase58,
    TestBech32,
    TestBloomFilter,
    TestBytesToBits,
    TestMerkleBlock
  )
  def main(args: Array[String]): Unit = {
    objects.foreach { o =>
      o.main(Array())
    }
  }
}
