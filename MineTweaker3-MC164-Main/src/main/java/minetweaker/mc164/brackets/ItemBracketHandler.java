/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package minetweaker.mc164.brackets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import minetweaker.IBracketHandler;
import minetweaker.MineTweakerAPI;
import minetweaker.annotations.BracketHandler;
import minetweaker.api.item.IItemStack;
import minetweaker.api.item.IngredientAny;
import static minetweaker.api.minecraft.MineTweakerMC.getIItemStackWildcardSize;
import net.minecraft.item.Item;
import net.minecraftforge.oredict.OreDictionary;
import stanhebben.zenscript.ZenTokener;
import stanhebben.zenscript.expression.ExpressionInt;
import stanhebben.zenscript.expression.ExpressionJavaCallStatic;
import stanhebben.zenscript.expression.ExpressionString;
import stanhebben.zenscript.expression.partial.IPartialExpression;
import stanhebben.zenscript.parser.Token;
import stanhebben.zenscript.symbols.IZenSymbol;
import stanhebben.zenscript.type.ZenType;
import stanhebben.zenscript.type.natives.JavaMethod;
import stanhebben.zenscript.util.ZenPosition;

/**
 *
 * @author Stan
 */
@BracketHandler(priority=100)
public class ItemBracketHandler implements IBracketHandler {
	private static final Map<String, Item> itemNames;
	
	static {
		itemNames = new HashMap<String, Item>();
		for (Item item : Item.itemsList) {
			if (item == null) continue;
			
			itemNames.put(item.getUnlocalizedName(), item);
		}
	}
	
	public static IItemStack getItem(String name, int meta) {
		Item item = itemNames.get(name);
		if (item != null) {
			return getIItemStackWildcardSize(item, meta);
		} else {
			return null;
		}
	}
	
	public static IItemStack getItem(int id, int meta) {
		Item item = Item.itemsList[id];
		if (item != null) {
			return getIItemStackWildcardSize(item, meta);
		} else {
			return null;
		}
	}
	
	private final IZenSymbol symbolAny;
	private final JavaMethod method;
	private final JavaMethod method2;
	
	public ItemBracketHandler() {
		symbolAny = MineTweakerAPI.getJavaStaticFieldSymbol(
				IngredientAny.class,
				"INSTANCE");
		method = MineTweakerAPI.getJavaMethod(
				ItemBracketHandler.class,
				"getItem",
				String.class, int.class);
		method2 = MineTweakerAPI.getJavaMethod(
				ItemBracketHandler.class,
				"getItem",
				int.class, int.class);
	}

	@Override
	public IZenSymbol resolve(List<Token> tokens) {
		// any symbol
		if (tokens.size() == 1 && tokens.get(0).getValue().equals("*")) {
			return symbolAny;
		}
		
		// detect special cases:
		//   item: at the start means item-specific syntax
		//   :xxx with xxx an integer means sub-item syntax
		//   :* means any-subitem-syntax
		int fromIndex = 0;
		int toIndex = tokens.size();
		int meta = 0;
		
		if (tokens.size() > 2) {
			if (tokens.get(0).getValue().equals("item") && tokens.get(1).getValue().equals(":")) {
				fromIndex = 2;
			}
			if (tokens.get(tokens.size() - 1).getType() == ZenTokener.T_INTVALUE
					&& tokens.get(tokens.size() - 2).getValue().equals(":")) {
				toIndex = tokens.size() - 2;
				meta = Integer.parseInt(tokens.get(tokens.size() - 1).getValue());
			} else if (tokens.get(tokens.size() - 1).getValue().equals("*")
					&& tokens.get(tokens.size() - 2).getValue().equals(":")) {
				toIndex = tokens.size() - 2;
				meta = OreDictionary.WILDCARD_VALUE;
			}
		}
		
		return find(tokens, fromIndex, toIndex, meta);
	}
	
	private IZenSymbol find(List<Token> tokens, int startIndex, int endIndex, int meta) {
		if (tokens.size() == 1 && tokens.get(0).getType() == ZenTokener.T_INTVALUE) {
			int id = Integer.parseInt(tokens.get(0).getValue());
			if (id < 0 || id > Item.itemsList.length || Item.itemsList[id] == null) {
				return null;
			}
			
			return new ItemReferenceSymbolId(id, meta);
		}
		
		StringBuilder valueBuilder = new StringBuilder();
		for (int i = startIndex; i < endIndex; i++) {
			Token token = tokens.get(i);
			valueBuilder.append(token.getValue());
		}
		
		if (itemNames.containsKey(valueBuilder.toString())) {
			return new ItemReferenceSymbol(valueBuilder.toString(), meta);
		}
		
		return null;
	}
	
	private class ItemReferenceSymbol implements IZenSymbol {
		private final String name;
		private final int meta;
		
		public ItemReferenceSymbol(String name, int meta) {
			this.name = name;
			this.meta = meta;
		}
		
		@Override
		public IPartialExpression instance(ZenPosition position) {
			return new ExpressionJavaCallStatic(
					position,
					method,
					new ExpressionString(position, name),
					new ExpressionInt(position, meta, ZenType.INT));
		}
	}
	
	private class ItemReferenceSymbolId implements IZenSymbol {
		private final int id;
		private final int meta;
		
		public ItemReferenceSymbolId(int id, int meta) {
			this.id = id;
			this.meta = meta;
		}
		
		@Override
		public IPartialExpression instance(ZenPosition position) {
			return new ExpressionJavaCallStatic(
					position,
					method2,
					new ExpressionInt(position, id, ZenType.INT),
					new ExpressionInt(position, meta, ZenType.INT));
		}
	}
}
