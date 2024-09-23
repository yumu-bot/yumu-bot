package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class ServiceSwitchException extends TipsException {
    public enum Type {
        SW_Instructions("""
        欢迎使用 Yumu Switch 功能！食用方法：
        !ymswitch / !sw (:group?) (service?) (operate?)
        group：需要限定的群聊。如果限定群聊，则必须输入服务名称和操作。
           如果输入 0，则可以清除当前服务的所有黑白名单。
        service：服务名称。
        operate：操作，可以输入 start, s, stop, p, end, e, on, o, off, f。用于开启或关闭服务。
           如果不输入服务名称，可以输入 list, review, l, r。用于查看服务开关状态。
           如果不输入操作，则默认查看服务开关状态。
        """),
        SW_Parameter_OnlyGroup("如果输入了群聊，则必须输入服务名称和操作！"),
        SW_Service_Missing("未输入服务名！"),
        SW_Service_NotFound("找不到名为 %s 的服务！"),
        SW_Service_RemoveNotExists("不需要解封本来就没被封禁的群组！"),
        SW_Service_AddExists("不需要封禁本来就已经被封禁的群组！"),
        SW_Render_Failed("服务列表渲染失败。"),

        ;
        public final String message;

        Type(String message) {
            this.message = message;
        }
    }

    public ServiceSwitchException(ServiceSwitchException.Type type) {
        super(type.message);
    }

    public ServiceSwitchException(ServiceSwitchException.Type type, Object... args) {
        super(String.format(type.message, args));
    }
}