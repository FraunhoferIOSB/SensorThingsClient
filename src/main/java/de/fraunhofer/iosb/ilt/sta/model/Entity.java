package de.fraunhofer.iosb.ilt.sta.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fraunhofer.iosb.ilt.sta.dao.BaseDao;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract representation of an entity. Entities are considered equal when
 * all entity properties (non-navigation properties) are equal.
 *
 * @author Nils Sommer, Hylke van der Schaaf
 * @param <T> The type of the entity implementing this interface
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Entity<T extends Entity<T>> {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Entity.class);

    @JsonProperty(value = "@iot.id")
    protected Id id;

    @JsonProperty(value = "@iot.selfLink", access = JsonProperty.Access.READ_ONLY)
    protected URI selfLink;

    /**
     * The entity type.
     */
    @JsonIgnore
    private final EntityType type;
    /**
     * The service this thing belong to.
     */
    @JsonIgnore
    private SensorThingsService service;

    public Entity(EntityType type) {
        this.type = type;
    }

    public Entity(EntityType type, Id id) {
        this.type = type;
        this.id = id;
    }

    @JsonAnySetter
    public void handleNamespacedProperties(String name, Object value) {
        String[] split = name.split("@", 2);
        if (split.length < 2) {
            LOGGER.info("Ignoring unknown property {}.", name);
            return;
        }
        if ("iot.selfLink".equals(split[1])) {
            setSelfLink(value.toString());
            return;
        }
        EntityType entityType = EntityType.byName(split[0]);
        if (entityType == null) {
            LOGGER.info("Unknown entity type '{}' for property '{}'", entityType, name);
            return;
        }
        try {
            Method method = getClass().getMethod("get" + entityType.getName(), (Class<?>[]) null);
            Object linkedEntity = method.invoke(this, (Object[]) null);
            if (linkedEntity instanceof EntityList) {
                EntityList entityList = (EntityList) linkedEntity;
                switch (split[1]) {
                    case "iot.count":
                        if (value instanceof Number) {
                            entityList.setCount(((Number) value).longValue());
                        } else {
                            LOGGER.error("{} should have numeric value, not {}", name, value);
                        }
                        break;

                    case "iot.nextLink":
                        entityList.setNextLink(URI.create(value.toString()));
                        break;

                    case "iot.navigationLink":
                        // ignored
                        break;
                }
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOGGER.info("Unknown entity type '{}' for property '{}'", entityType, name);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Entity<?> other = (Entity<?>) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return this.type == other.type;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + Objects.hashCode(this.id);
        hash = 13 * hash + Objects.hashCode(this.type);
        return hash;
    }

    public EntityType getType() {
        return type;
    }

    public Id getId() {
        return this.id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public URI getSelfLink() {
        return this.selfLink;
    }

    public void setSelfLink(String selfLink) {
        try {
            this.selfLink = URI.create(selfLink);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid selflink: {}", selfLink);
        }
    }

    public void setSelfLink(URI selfLink) {
        this.selfLink = selfLink;
    }

    public void setService(SensorThingsService service) {
        if (this.service != service) {
            this.service = service;
            ensureServiceOnChildren(service);
        }
    }

    protected abstract void ensureServiceOnChildren(SensorThingsService service);

    public SensorThingsService getService() {
        return service;
    }

    public abstract BaseDao<T> getDao(SensorThingsService service);

    /**
     * Creates a copy of the entity, with only the ID field set. Useful when
     * creating a new entity that links to this entity.
     *
     * @return a copy with only the ID field set.
     */
    public abstract T withOnlyId();

    @Override
    public String toString() {
        if (id == null) {
            return "no id";
        }
        return id.toString();
    }

}
