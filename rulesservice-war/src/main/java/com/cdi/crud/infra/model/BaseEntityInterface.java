package com.cdi.crud.infra.model;

import java.io.Serializable;

/**
 *
 * @author rmpestano
 */
public interface BaseEntityInterface extends Serializable{


    public <T extends Serializable> T getId();

}