package io.github.terra121.control;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

import io.github.terra121.TerraMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.resources.I18n;

public class DynamicOptions extends GuiSlot { 
	private Element[] elements;
	private Handler handler;
	
    public DynamicOptions(Minecraft mcIn, int width, int height, int top, int bottom, int slotsize, Handler handler, Element[] elems)
    {
        super(mcIn, width, height, top, bottom, slotsize);
        elements = elems;
        this.handler = handler;
    }

    protected int getSize()
    {
        return elements.length;
    }

    /**
     * The element in the slot that was clicked, boolean for whether it was double clicked or not
     * This don't work idk why, so using mouseClicked
     */
    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY)
    {
    }
    
    //click the proper button
    public void mouseClicked(int mouseX, int mouseY, int mouseEvent)
    {
        if (this.isMouseYWithinSlotBounds(mouseY))
        {
            int i = this.getSlotIndexFromScreenCoords(mouseX, mouseY);

            if (i >= 0)
            {
                elements[i].click(mc);
                if(handler!=null)
                	handler.onDynOptClick(elements[i]);
            }
        }
    }

    /**
     * Returns true if the element passed in is currently selected
     */
    protected boolean isSelected(int slotIndex)
    {
        return false;
    }

    protected void drawBackground()
    {
        //EarthGui.this.drawDefaultBackground();
    }
    
    protected void drawSlot(int slotIndex, int xPos, int yPos, int heightIn, int mouseXIn, int mouseYIn, float partialTicks)
    {
        elements[slotIndex].draw(mc, xPos, yPos, heightIn, mouseXIn, mouseYIn, partialTicks);
    }
    
    public abstract static class Element {
    	public abstract void draw(Minecraft mc, int x, int y, int height, int mouseX, int mouseY, float partialTicks);
    	public void click(Minecraft mc) {}
    }

    public static class ConditionalShowElement extends Element {
    	Element element;
		Function<Void, Boolean> shouldDraw;

		public ConditionalShowElement(Element element, Function<Void, Boolean> shouldDraw) {
			this.element = element;
			this.shouldDraw = shouldDraw;
		}

		@Override
		public void draw(Minecraft mc, int x, int y, int height, int mouseX, int mouseY, float partialTicks) {
			if (shouldDraw.apply(null)) {
				element.draw(mc, x, y, height, mouseX, mouseY, partialTicks);
			}
		}

		public void click(Minecraft mc) {
			if (shouldDraw.apply(null)) {
				element.click(mc);
			}
		}
	}
    
    public static class CycleButtonElement<E> extends Element {
    	public GuiButton gui;
    	public E[] options;
    	public int current;
    	Function<E, String> tostring;
    	Field outf;
    	Object outo;
    	
    	public CycleButtonElement(int id, E[] options, Field outfield, Object outobject, Function<E, String> tostring){
    		this.outo = outobject;
    		this.outf = outfield;
    		this.options = options;
    		this.tostring = tostring;
    		
    		try {
    			current = Arrays.asList(options).indexOf(outfield.get(outobject));
    		} catch (IllegalAccessException e) {
				TerraMod.LOGGER.error("This should never happen, but get reflection error");
				e.printStackTrace();
			}
    		
    		gui = new GuiButton(id, 0, 0, tostring.apply(options[current]));
    	}
    	
    	public void click(Minecraft mc) {
    		current++;
    		if(current>=options.length)
    			current = 0;
    		
    		try {
				outf.set(outo, options[current]);
			} catch (IllegalAccessException e) {
				TerraMod.LOGGER.error("This should never happen, but set reflection error");
				e.printStackTrace();
			}
    		
    		gui.displayString = tostring.apply(options[current]);
    	}
    	
    	public void draw(Minecraft mc, int x, int y, int height, int mouseX, int mouseY, float partialTicks) {
    		gui.height = height>20?20:height;
    		gui.x = x;
    		gui.y = y;
    		gui.drawButton(mc, mouseX, mouseY, partialTicks);
    	}
    }
    
    //quick wrapper for a yes or no toggle
    public static class ToggleElement extends CycleButtonElement<Boolean> {
    	public ToggleElement(int id, String name, Field outfield, Object outobject, Consumer<Boolean> notify){
    		super(id, new Boolean[] {false, true}, outfield, outobject, b -> {
	    			if(notify!=null)
	    				notify.accept(b);
	    			return name+": "+(b?I18n.format("options.on"):I18n.format("options.off"));
    			});
    	}
    }
    
    public static interface Handler {
    	public void onDynOptClick(Element elem);
    }
}
