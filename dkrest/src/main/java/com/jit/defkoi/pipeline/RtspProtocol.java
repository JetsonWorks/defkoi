package com.jit.defkoi.pipeline;

import lombok.Getter;

public enum RtspProtocol {
  udp(1), udpMcast(2), tcp(4), http(16), tls(32);

  @Getter
  private int bit;

  RtspProtocol(int bit) {
    this.bit = bit;
  }

  public static int bitOr(RtspProtocol... protocols) {
    int value = 0;
    for(RtspProtocol p : protocols) {
      value |= p.getBit();
    }
    return value;
  }

}
