package io.kuzzle.demo.demo_android.enums;

/**
 * Created by kblondel on 16/12/15.
 */
public enum Status {

  IDLE("idle"),
  WANTTOHIRE("wantToHire"),
  TOHIRE("toHire"),
  RIDING("riding"),
  HIRED("hired");

  private String  value;

  Status(String value) {
    this.value = value;
  }

  public String toString() {
    return value;
  }

}
