package org.sh.cryptonode.bch

import org.sh.cryptonode.btc.PrvKey

object BitcoinCashPrvKey {
  def getPrvKeyP2PKH_UAHF(wif:String) = {
    val (eccPrvKey, mainNet) = PrvKey.getECCPrvKeyAndNet(wif)
    new PrvKey_P2PKH_UAHF(eccPrvKey, mainNet)
  }
}