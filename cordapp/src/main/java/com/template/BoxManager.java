package com.template;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;

import java.util.*;

public class BoxManager {
    public static boolean isEnoughBox(StateAndRef<Box> boxes, Integer num){


    }
    //private VaultService vaultService;

    //public BoxManager(VaultService vaultService) {
    //    this.vaultService = vaultService;
    //}

    public static List<StateAndRef<Box>> getBoxByType(String productType, ServiceHub serviceHub) {

//        Iterable<StateAndRef<SecurityBasket>> vaultStates = vaultService.queryBy(SecurityBasket.class);
//        Vault.Page<Box> result = vaultService.queryBy(Box.class);
//        Iterable<StateAndRef<Box>> vaultStates = result.getStates();
//
//        StateAndRef<Box> inputStateAndRef = null;
//
//        Iterator<StateAndRef<Box>> it;
//        it = vaultStates.iterator();
//        while (it.hasNext()) {
//            StateAndRef<Box> stateAndRef = it.next();
//            if (stateAndRef.getState().getData().getBasketIpfsHash().equals(basketIpfsHash)) {
//                inputStateAndRef = stateAndRef;
//                break;
//            }
//        }
//        return inputStateAndRef;

        Vault.Page<Box> boxPage = serviceHub.getVaultService().queryBy(Box.class);
        resyt boxPage.component1().stream().anyMatch(e -> e.getState().getData()
                .getProductType().equalsIgnoreCase(productType));
    }


    public static int numOfBox(StateAndRef<Box> boxes) {
        int rest =0;
        Iterable<StateAndRef<Box>> vaultStates = boxes.getStates();

        Iterator<StateAndRef<Box>> it;
        it = vaultStates.iterator();
        while (it.hasNext()) {
            StateAndRef<Box> stateAndRef = it.next();
            if (stateAndRef.getState().getData().getBasketIpfsHash().equals(basketIpfsHash)) {
                inputStateAndRef = stateAndRef;
                break;
            }
        }
        return inputStateAndRef;
    }
}