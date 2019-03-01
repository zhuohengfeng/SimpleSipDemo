package com.rokid.simplesip.gb28181;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.converters.reflection.FieldKeySorter;

import java.util.Map;

/**
 <Response>
 <CmdType>Catalog</CmdType>
 <SN>42947600</SN>
 <DeviceID>33080002001326031122</DeviceID>
 <SumNum>1</SumNum>
 <DeviceList Num="1">
    <Item>
         <DeviceID>33080002001326031122</DeviceID>
         <Name>RokidName</Name>
         <Manufacturer>Rokid</Manufacturer>
         <Model>Rokid</Model>
         <Owner>Rokid</Owner>
         <CivilCode>01234567</CivilCode>
         <Address>Rokid</Address>
         <Parental>0</Parental>
         <SafetyWay>0</SafetyWay>
         <RegisterWay>1</RegisterWay>
         <Secrecy>0</Secrecy>
         <IPAddress>192.168.1.164</IPAddress>
         <Port>80</Port>
         <Password>9999</Password>
         <Status>ON</Status>
     </Item>
 </DeviceList>
 </Response>
*/

/**
 * Author: zhuohf
 * Version: V0.1 2018/2/19
 */
@XMLSequence({"CmdType", "SN", "DeviceID", "Result", "DeviceType", "Manufacturer", "Model", "Firmware", "SumNum", "DeviceList"})
@XStreamAlias("Response")
public class ResponseMessage extends BaseMessage implements FieldKeySorter {

    private String SumNum;

    private DeviceList DeviceList;

    public String getSumNum() {
        return SumNum;
    }

    public void setSumNum(String sumNum) {
        SumNum = sumNum;
    }

    public DeviceList getDeviceList() {
        return DeviceList;
    }

    public void setDeviceList(DeviceList deviceList) {
        DeviceList = deviceList;
    }

    @Override
    public String toString() {
        return "ResponseMessage{" +
                "SumNum='" + SumNum + '\'' +
                ", DeviceList=" + DeviceList +
                ", CmdType='" + CmdType + '\'' +
                ", SN='" + SN + '\'' +
                ", DeviceID='" + DeviceID + '\'' +
                ", Status='" + Status + '\'' +
                '}';
    }

    @Override
    public Map sort(Class type, Map keyedByFieldKey) {
        return null;
    }
}
