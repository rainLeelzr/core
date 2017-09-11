package avatar.rain.core.controller;

import avatar.rain.core.dao.BaseDao;
import avatar.rain.core.entity.BaseEntity;
import avatar.rain.core.service.BaseService;

public abstract class BaseController<S extends BaseService<E, D>, D extends BaseDao<E>, E extends BaseEntity> {

    protected abstract S getService();

    public E getEntityById(String id) {
        return getService().getById(id);
    }
}
