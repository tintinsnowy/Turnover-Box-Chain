package com.template;

import com.esotericsoftware.kryo.NotNull;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.finance.contracts.CommercialPaper;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.lang.*;

public class Box implements OwnableState {

    private PartyAndReference issuance;
    private AbstractParty owner;
    private String productType;
    private long num;
    //What is series
//    public BoxState( AnonymousParty owner, String productType, double price) {
//    }  // For serialization

    @ConstructorForDeserialization
    public Box( AbstractParty owner, //Amount<Issued<Currency>> faceValue , Instant maturityDate) {
                    String productType, long num){

        this.owner = owner;
        this.productType = productType;
        this.num = num;
    }
    public Box copy() {
        return new Box(this.owner, this.productType,this.num);
    }

    public Box withoutOwner() {
        return new Box(new AnonymousParty(NullKeys.NullPublicKey.INSTANCE),
                this.productType, this.num);
    }

    @Override
    public CommandAndState withNewOwner(AbstractParty newOwner) {
        return new CommandAndState(new AddBoxContract.Commands.Transfer(),
                new Box(newOwner, this.productType, this.num));
    }

    public String getProductType() {
        return productType;
    }

    public AbstractParty getOwner() {
        return owner;
    }

    public long getNum() {
        return num;
    }


    @Override
   public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Box state = (Box) o;

        if (owner != null ? !owner.equals(state.owner) : state.owner != null) return false;
        if (productType != null ? !productType.equals(state.productType) : state.productType != null) return false;
        return !(num <0 ? !(num==(state.num)) : state.num < 0 ); //-----to be checked
    }

    @Override
    public int hashCode() {
        //int result = issuance != null ? issuance.hashCode() : 0;
        int result =  owner != null ? owner.hashCode() : 0;
        //result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (num >0 ? Double.toString(num).hashCode() : 0);
        //result = 31 * result + (maturityDate != null ? maturityDate.hashCode() : 0);
        return result;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        // an ImmutableList is a List whose contents are immutable so can’t be modified.
        return ImmutableList.of(this.owner);
    }


}
