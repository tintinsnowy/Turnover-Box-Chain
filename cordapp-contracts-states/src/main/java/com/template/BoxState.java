package com.template;

import com.esotericsoftware.kryo.NotNull;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.finance.contracts.CommercialPaper;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.lang.*;



public class BoxState implements OwnableState {

    //private PartyAndReference issuance;
    private AbstractParty owner;
    //private Amount<Issued<Currency>> faceValue;
    private double faceValue;
    //private Instant maturityDate;
    private String productType;
    private double price;
    private Integer num;

    public BoxState( AnonymousParty owner, String productType, double price) {
    }  // For serialization

    public BoxState( AbstractParty owner, //Amount<Issued<Currency>> faceValue , Instant maturityDate) {
                    String productType, double price){

        //this.issuance = issuance;

        this.owner = owner;
        this.faceValue = price * num;
        //this.maturityDate = maturityDate;
        this.productType = productType;
        this.price = price;
        //this.num = num;
    }

    public BoxState copy() {
        return new BoxState(this.owner, this.productType,this.price);
    }

    public BoxState withoutOwner() {
        return new BoxState(new AnonymousParty(NullKeys.NullPublicKey.INSTANCE),
                this.productType, this.price);
    }

    @Override
    public CommandAndState withNewOwner(AbstractParty newOwner) {
        return new CommandAndState(new CommercialPaper.Commands.Move(),
                new BoxState(newOwner, this.productType, this.price));
    }

   /* public PartyAndReference getIssuance() {
        return issuance;
    }*/

    public AbstractParty getOwner() {
        return owner;
    }

    public double getFaceValue() {
        return faceValue;
    }
//
//    public Instant getMaturityDate() {
//        return maturityDate;
//    }

 /*
    @Override
   public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoxState state = (BoxState) o;

        if (issuance != null ? !issuance.equals(state.issuance) : state.issuance != null) return false;
        if (owner != null ? !owner.equals(state.owner) : state.owner != null) return false;
        return !(faceValue != null ? !faceValue.equals(state.faceValue) : state.faceValue != null);
        //!(maturityDate != null ? !maturityDate.equals(state.maturityDate) : state.maturityDate != null);
    }*/

    @Override
    public int hashCode() {
        //int result = issuance != null ? issuance.hashCode() : 0;
        int result =  owner != null ? owner.hashCode() : 0;
        //result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (price >0 ? Double.toString(price).hashCode() : 0);
        //result = 31 * result + (maturityDate != null ? maturityDate.hashCode() : 0);
        result = 31 * result + (num >0 ? Integer.toString(num).hashCode() : 0);
        return result;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        // an ImmutableList is a List whose contents are immutable so can’t be modified.
        return ImmutableList.of(this.owner);
    }
}