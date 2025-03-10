package minihud.renderer.shapes;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import malilib.config.value.BaseOptionListConfigValue;
import malilib.listener.LayerRangeChangeListener;
import malilib.util.StringUtils;
import malilib.util.data.Color4f;
import malilib.util.data.json.JsonUtils;
import malilib.util.game.wrap.GameUtils;
import malilib.util.position.LayerRange;
import minihud.config.RendererToggle;
import minihud.renderer.MiniHudOverlayRenderer;
import minihud.util.value.ShapeRenderType;

public abstract class ShapeBase extends MiniHudOverlayRenderer implements LayerRangeChangeListener
{
    protected final Minecraft mc = GameUtils.getClient();
    protected final ShapeType type;
    protected final LayerRange layerRange;
    protected String displayName;
    protected ShapeRenderType renderType;
    protected Color4f color;
    protected boolean enabled;

    public ShapeBase(ShapeType type, Color4f color)
    {
        this.type = type;
        this.color = color;
        this.layerRange = new LayerRange(this);
        this.renderType = ShapeRenderType.OUTER_EDGE;
        this.displayName = type.getDisplayName();
        this.needsUpdate = true;
    }

    public ShapeType getType()
    {
        return this.type;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }

    public LayerRange getLayerRange()
    {
        return this.layerRange;
    }

    public Color4f getColor()
    {
        return this.color;
    }

    public ShapeRenderType getRenderType()
    {
        return this.renderType;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public void setRenderType(ShapeRenderType renderType)
    {
        this.renderType = renderType;
        this.setNeedsUpdate();
    }

    public void setColor(int newColor)
    {
        if (newColor != this.color.intValue)
        {
            this.color = Color4f.fromColor(newColor);
            this.setNeedsUpdate();
        }
    }

    public void setColorFromString(String newValue)
    {
        this.setColor(Color4f.getColorFromString(newValue, 0));
    }

    public boolean isShapeEnabled()
    {
        return this.enabled;
    }

    public void toggleEnabled()
    {
        this.enabled = ! this.enabled;

        if (this.enabled)
        {
            this.setNeedsUpdate();
        }
    }

    @Override
    public boolean shouldRender()
    {
        return this.enabled && RendererToggle.SHAPE_RENDERER.isRendererEnabled();
    }

    @Override
    public boolean needsUpdate(Entity entity)
    {
        return this.needsUpdate;
    }

    @Override
    public void updateAll()
    {
        this.setNeedsUpdate();
    }

    @Override
    public void updateBetweenX(int minX, int maxX)
    {
        this.setNeedsUpdate();
    }

    @Override
    public void updateBetweenY(int minY, int maxY)
    {
        this.setNeedsUpdate();
    }

    @Override
    public void updateBetweenZ(int minZ, int maxZ)
    {
        this.setNeedsUpdate();
    }

    public List<String> getWidgetHoverLines()
    {
        List<String> lines = new ArrayList<>();

        lines.add(StringUtils.translate("minihud.hover.shape.type", this.type.getDisplayName()));

        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

        obj.add("type", new JsonPrimitive(this.type.getId()));
        obj.add("color", new JsonPrimitive(this.color.intValue));
        obj.add("enabled", new JsonPrimitive(this.enabled));
        obj.add("display_name", new JsonPrimitive(this.displayName));
        obj.add("render_type", new JsonPrimitive(this.renderType.getName()));
        obj.add("layers", this.layerRange.toJson());

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        this.enabled = JsonUtils.getBoolean(obj, "enabled");

        if (JsonUtils.hasInteger(obj, "color"))
        {
            this.color = Color4f.fromColor(JsonUtils.getInteger(obj, "color"));
        }

        if (JsonUtils.hasObject(obj, "layers"))
        {
            this.layerRange.fromJson(JsonUtils.getNestedObject(obj, "layers", false));
        }

        if (JsonUtils.hasString(obj, "render_type"))
        {
            ShapeRenderType type = BaseOptionListConfigValue.findValueByName(obj.get("render_type").getAsString(), ShapeRenderType.VALUES);

            if (type != null)
            {
                this.renderType = type;
            }
        }

        if (JsonUtils.hasString(obj, "display_name"))
        {
            this.displayName = obj.get("display_name").getAsString();
        }
    }
}
