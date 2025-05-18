package requests.skelethon.interfaces;

import models.BaseModel;

public interface CrudEndpointInterface {
    Object post(BaseModel model);
    Object put(BaseModel model);
    Object get(long id);
    Object getAll();
    Object update(long id, BaseModel model);
    Object delete(long id);
}
