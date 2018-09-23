package com.template;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.*;

public class BoxManager {

    public static StateAndRef<Box> getOneBoxByType(String productType, ServiceHub serviceHub) {
        System.out.print("checking balance for " + productType);
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        Vault.Page<Box> boxPage = serviceHub.getVaultService().queryBy(Box.class,criteria);
        return boxPage.getStates().stream().filter(e -> e.getState().
                getData().getProductType().equalsIgnoreCase(productType)).findAny().
                orElseThrow(() -> new NotFoundException("No Box found with productType: " + productType));
    }

    public static int getBoxBalance(String productType, ServiceHub serviceHub) {
        List<StateAndRef<Box>> Boxes = getBoxesByType(productType, serviceHub);
        System.out.println("balance for " + productType + " is " + Boxes.size());
        return Boxes.size();
    }

    public static List<StateAndRef<Box>> getBoxesByType(String productType, ServiceHub serviceHub) {
        List<StateAndRef<Box>> vaultStates = serviceHub.getVaultService().queryBy(Box.class).getStates();
        List<StateAndRef<Box>> Boxes = new ArrayList<>();
        Iterator<StateAndRef<Box>> it;
        it = vaultStates.iterator();
        while(it.hasNext()) {
            StateAndRef<Box> stateAndRef = it.next();
            if (stateAndRef.getState().getData().getProductType().equals(productType)) {
                Boxes.add(stateAndRef);
            }
        }
        return Boxes;
    }
}