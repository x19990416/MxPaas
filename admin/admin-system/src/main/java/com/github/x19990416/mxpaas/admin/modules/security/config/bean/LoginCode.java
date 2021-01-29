/*
 *  Copyright (c) 2020-2021 Guo Limin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.x19990416.mxpaas.admin.modules.security.config.bean;

import lombok.Data;

@Data
public class LoginCode {
	/**
	 * 验证码配置
	 */
	private LoginCodeEnum codeType;
	/**
	 * 验证码有效期 分钟
	 */
	private Long expiration = 2L;
	/**
	 * 验证码内容长度
	 */
	private int length = 2;
	/**
	 * 验证码宽度
	 */
	private int width = 111;
	/**
	 * 验证码高度
	 */
	private int height = 36;
	/**
	 * 验证码字体
	 */
	private String fontName;
	/**
	 * 字体大小
	 */
	private int fontSize = 25;

	public LoginCodeEnum getCodeType() {
		return codeType;
	}
}
