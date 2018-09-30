package com.template;

import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

import java.security.PublicKey;
import java.util.Currency;
import java.util.List;

//Contract classes must provide a verify function, but they may optionally also provide helper functions to simplify their usage.
@CordaSerializable
public class Helper
{
    /*
    By default, for security purposes, only classes present on the default serialization whitelist can be sent within flows or over RPC.
    There are two ways to add a specific class to the serialization whitelist.
     */
    @CordaSerializable
    public static class LenderInfo {
        public final Amount<Currency> amount;
        public final Party payToIdentity;

        public LenderInfo(Amount<Currency> amount, Party payToIdentity) {
            this.amount = amount;
            this.payToIdentity = payToIdentity;
        }
    }

}
