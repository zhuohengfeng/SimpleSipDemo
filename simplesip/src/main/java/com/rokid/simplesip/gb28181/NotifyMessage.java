package com.rokid.simplesip.gb28181;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Author: zhuohf
 * Version: V0.1 2018/2/19
 */
@XMLSequence({"CmdType", "SN", "DeviceID", "Status"})
@XStreamAlias("Notify")
public class NotifyMessage extends BaseMessage {
}
