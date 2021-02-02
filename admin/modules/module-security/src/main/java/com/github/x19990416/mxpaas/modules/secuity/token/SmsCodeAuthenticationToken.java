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
package com.github.x19990416.mxpaas.modules.secuity.token;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class SmsCodeAuthenticationToken extends AbstractAuthenticationToken {

  private final Object principal;
  private Object credentials;

  public SmsCodeAuthenticationToken(String mobile) {
    super(null);
    this.principal = mobile;
    setAuthenticated(false);
  }

  public SmsCodeAuthenticationToken(
      Object principal, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    super.setAuthenticated(true); // must use super, as we override
  }

  public void eraseCredentials() {
    super.eraseCredentials();
    this.credentials = null;
  }

  @Override
  public Object getCredentials() {
    return this.credentials;
  }

  @Override
  public Object getPrincipal() {
    return this.principal;
  }

  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    if (isAuthenticated) {
      throw new IllegalArgumentException(
          "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
    }
    super.setAuthenticated(false);
  }
}
