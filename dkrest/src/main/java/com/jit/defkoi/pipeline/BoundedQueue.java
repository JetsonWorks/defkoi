package com.jit.defkoi.pipeline;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BoundedQueue<E> extends ConcurrentLinkedQueue<E> {

  private final int size;

  public BoundedQueue(final int size) {
    this.size = size;
  }

  @Override
  public boolean add(final E object) {
    super.add(object);
    while(size() > size) {
      poll();
    }
    return size > 0;
  }
}
