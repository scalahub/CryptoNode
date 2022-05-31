package org.sh.cryptonode.util

object Bech32 {
  /*
https://github.com/bitcoin/bips/blob/master/bip-0173.mediawiki
https://gist.github.com/scalahub/3ff855c3404f7aef953d0c079cf71901

	  0	1	2	3	4	5	6	7
+0	q	p	z	r	y	9	x	8
+8	g	f	2	t	v	d	w	0
+16	s	3	j	n	5	4	k	h
+24	c	e	6	m	u	a	7	l
   */

  val sep = '1' // separator of hrp and data part

  val str2IntMap: Map[Char, Int] = Seq(
    'q', 'p', 'z', 'r', 'y', '9', 'x', '8', // row 1
    'g', 'f', '2', 't', 'v', 'd', 'w', '0', // row 2
    's', '3', 'j', 'n', '5', '4', 'k', 'h', // row 3
    'c', 'e', '6', 'm', 'u', 'a', '7', 'l' // row 4
  ).zipWithIndex.toMap

  val int2StrMap: Map[Int, Char] = str2IntMap.map(_.swap)

  def toInt(bits: String) = Integer.valueOf(bits, 2).toInt

  def getInts(string: String): Seq[Int] = {
    string.map(str2IntMap)
  }

  private val GEN: Seq[Int] =
    Seq(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

  def polyMod(values: Seq[Int]): Int = {
    var chk = 1
    for (v <- values) {
      val b = chk >>> 25
      chk = ((chk & 0x1ffffff) << 5) ^ v
      for (i <- 0 to 5) {
        if (((b >>> i) & 1) != 0) chk = chk ^ GEN(i)
      }
    }
    chk
  }

  def hrpExpand(s: String): Seq[Int] = {
    s.map(_ >>> 5) ++ Seq(0) ++ s.map(_ & 31)
  }

  def verifyChecksum(hrp: String, data: String): Boolean = {
    polyMod(hrpExpand(hrp) ++ getInts(data)) == 1
  }

  def createChecksum(hrp: String, data: Seq[Int]): Seq[Int] = {
    val values = hrpExpand(hrp) ++ data
    val polyM: Int = polyMod(values ++ Seq(0, 0, 0, 0, 0, 0)) ^ 1
    0 to 5 map { i =>
      (polyM >>> 5 * (5 - i)) & 31
    }
  }

  def encode(hrp: String, data: Seq[Int]): String = {
    require(hrp.nonEmpty, "hrp must be non-empty string")
    hrp + sep ++ (data ++ createChecksum(hrp, data)).map(int2StrMap)
  }

  def decode(bech32: String): (String, Seq[Int]) = {
    val l = bech32.length
    require(
      l >= 8 && l <= 90,
      s"Invalid Bech32: $bech32 (length $l). Valid length range: 8-90 characters."
    )
    require(
      bech32.forall(c => c.isLower || c.isDigit) || bech32.forall(c =>
        c.isUpper || c.isDigit
      ),
      s"Invalid Bech32: $bech32. Mixed case."
    )
    val sepPosition = bech32.lastIndexOf(sep)
    require(
      sepPosition != -1,
      s"Invalid Bech32: $bech32. Missing separator $sep."
    )
    val input = bech32.toLowerCase()
    val hrp = input.take(sepPosition)
    val data = input.drop(sepPosition + 1)
    require(
      hrp.length >= 1,
      s"Invalid Bech32: $bech32. Invalid hrp length ${hrp.length}."
    )
    require(
      data.length >= 6,
      s"Invalid Bech32: $bech32. Invalid data length ${data.length}."
    )
    require(verifyChecksum(hrp, data), s"Invalid checksum for $bech32")
    (hrp, data.dropRight(6).map(str2IntMap))
  }

  def toBase32(bytes: Array[Byte]): Seq[Int] = {
    val grouped: Array[String] =
      bytes
        .map(byte => ("0" * 8 + byte.toBinaryString).takeRight(8))
        .mkString("")
        .grouped(5)
        .toArray
    grouped(grouped.size - 1) = (grouped.last + "0" * 5).take(5)
    grouped.map(toInt)
  }

  def fromBase32(ints: Seq[Int]): Array[Byte] = {
    val grouped = ints
      .map(_.toBinaryString)
      .map(str => ("0" * 5 + str).takeRight(5))
      .mkString("")
      .grouped(8)
      .toArray

    val last = grouped.last

    val byteStrings: Array[String] = if (last.size < 8) {
      require(last.size <= 4, s"$last.size (${last.size}) is > 4")
      require(last.forall(_ == '0'), s"last has non-zero chars $last")
      grouped.dropRight(1)
    } else grouped

    byteStrings.map(toInt(_).toByte)
  }
}
