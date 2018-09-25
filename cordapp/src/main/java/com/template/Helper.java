package com.template;

import com.template.RefuelFeeState;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.asset.Cash;

import java.security.PublicKey;
import java.util.Currency;
import java.util.List;

//Contract classes must provide a verify function, but they may optionally also provide helper functions to simplify their usage.
public class Helper
{
    public static class LenderInfo {
        public final Amount<Currency> amount;
        public final Party payToIdentity;

        public LenderInfo(Amount<Currency> amount, Party payToIdentity) {
            this.amount = amount;
            this.payToIdentity = payToIdentity;
        }
    }

}
