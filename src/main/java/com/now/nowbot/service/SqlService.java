package com.now.nowbot.service;

import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;

@Service("sql")
public class SqlService {
    @PersistenceContext
    EntityManager em;

    public List<Map<String,Object>> queryBySql(String sql){
        Query query = em.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> list = query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
        return list;
    }


    @Transactional
    public void excuteSql(String sql){
        Query query = em.createNativeQuery(sql);
        query.executeUpdate();
    }
}
