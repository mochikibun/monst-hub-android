package com.mochikibun.monsthub;
/** Explicit fixture; production installations start with zero accounts. */
final class TestStates { static HubState twelve()throws Exception{HubState s=HubState.create();for(int i=1;i<=12;i++)s.addAccount(i==1?"メイン":"サブ"+(i-1));return s;} }
