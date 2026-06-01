package com.auction.framework.security;

/**
 * Checks whether a parsed JWT still belongs to an account that may access APIs.
 * Implemented outside framework so the security filter does not depend on the
 * system module directly.
 */
public interface AccountStatusChecker {

    /**
     * @return true when the account is active and the token may be accepted.
     */
    boolean isActive(Long userId);
}
