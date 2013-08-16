/*
  GRANITE DATA SERVICES
  Copyright (C) 2011 GRANITE DATA SERVICES S.A.S.

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 3 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
  for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package com.wineshop.services;

import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

import javax.persistence.Query;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.NoResultException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wineshop.entities.Welcome;


@Service
public class WelcomeServiceImpl implements WelcomeService {

    @PersistenceContext
    private EntityManager entityManager;
    

    @Transactional
    public Welcome hello(String name) {
        if (name == null || name.trim().length() == 0)
            throw new RuntimeException("Name cannot be null or empty");
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Welcome welcome = null;
        try {
            Query q = entityManager.createQuery("select w from Welcome w where w.name = :name");
            q.setParameter("name", name);
            welcome = (Welcome)q.getSingleResult();
            welcome.setMessage("Welcome " + name + " (" + sdf.format(new Date()) + ")");
        }
        catch (NoResultException e) {
            welcome = new Welcome();
            welcome.setName(name);
            welcome.setMessage("Welcome " + name + " (" + sdf.format(new Date()) + ")");
            entityManager.persist(welcome);
        }
        return welcome;
    }
    
    
    @Transactional(readOnly=true)
    public List<Welcome> findAll() {
        return entityManager.createQuery("select w from Welcome w order by w.name", Welcome.class).getResultList();
    }
}
