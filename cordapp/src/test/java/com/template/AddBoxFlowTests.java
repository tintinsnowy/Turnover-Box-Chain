package com.template;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.node.services.api.StartedNodeServices;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class AddBoxFlowTests {
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    private MockNetwork network;
    private StartedMockNode operator;
    //  private StartedMockNode notoray;

    @Before
    public void setup() {
        network = new MockNetwork(ImmutableList.of("com.template"));
        operator = network.createNode(CordaX500Name.parse("O=Operator,L=Cologne,C=DE"));
      //  operator = network.createNode();
        //CordaFuture<SignedTransaction> future = StartedNodeServices.startFlow(operator.getServices(), new AddBoxFlow("Normal",5));
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }


    @Test
    public void test() throws Exception {
        AddBoxFlow flow = new AddBoxFlow("Normal", 50);
        operator.startFlow(flow).toString();
        network.runNetwork();
    }
}
