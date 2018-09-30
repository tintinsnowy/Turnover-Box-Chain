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

// https://github.com/soumilvavikar/exchange-traded-funds/blob/f1a34335a3b7cf135f35ca350d44118cd5fd8c37/java-source/src/main/java/com/poc/State.java

public class Box implements OwnableState {

    private PartyAndReference issuance;
    private AbstractParty owner;
    //private Amount<Issued<Currency>> faceValue;
    //private Instant maturityDate;
    private String productType;
    private double price;


    //What is series
//    public BoxState( AnonymousParty owner, String productType, double price) {
//    }  // For serialization

    @ConstructorForDeserialization
    public Box( AbstractParty owner, //Amount<Issued<Currency>> faceValue , Instant maturityDate) {
                    String productType, double price){

        //this.issuance = issuance;
        this.owner = owner;
        //this.maturityDate = maturityDate;
        this.productType = productType;
        this.price = price;
    }

    public Box copy() {
        return new Box(this.owner, this.productType,this.price);
    }

    public Box withoutOwner() {
        return new Box(new AnonymousParty(NullKeys.NullPublicKey.INSTANCE),
                this.productType, this.price);
    }

    @Override
    public CommandAndState withNewOwner(AbstractParty newOwner) {
        return new CommandAndState(new AddBoxContract.Commands.Transfer(),
                new Box(newOwner, this.productType, this.price));
    }

    public String getProductType() {
        return productType;
    }

    public AbstractParty getOwner() {
        return owner;
    }



    @Override
   public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Box state = (Box) o;

        if (owner != null ? !owner.equals(state.owner) : state.owner != null) return false;
        if (productType != null ? !productType.equals(state.productType) : state.productType != null) return false;
        return !(price <0 ? !(price==(state.price)) : state.price < 0 ); //-----to be checked
    }

    @Override
    public int hashCode() {
        //int result = issuance != null ? issuance.hashCode() : 0;
        int result =  owner != null ? owner.hashCode() : 0;
        //result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (price >0 ? Double.toString(price).hashCode() : 0);
        //result = 31 * result + (maturityDate != null ? maturityDate.hashCode() : 0);
        return result;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        // an ImmutableList is a List whose contents are immutable so can’t be modified.
        return ImmutableList.of(this.owner);
    }


}
