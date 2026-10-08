package test.expressions;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.math.BigDecimal;
import java.util.Objects;
import test.expressions.meta.ItemMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Item", builder=Item.ItemBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Item", model="test", builder=Item.ItemBuilderImpl.class, version="0.0.0")
public interface Item extends RosettaModelObject {

	ItemMeta metaData = new ItemMeta();

	/*********************** Getter Methods  ***********************/
	String getName();
	BigDecimal getPrice();

	/*********************** Build Methods  ***********************/
	Item build();
	
	Item.ItemBuilder toBuilder();
	
	static Item.ItemBuilder builder() {
		return new Item.ItemBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Item> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Item> getType() {
		return Item.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		processor.processBasic(path.newSubPath("price"), BigDecimal.class, getPrice(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ItemBuilder extends Item, RosettaModelObjectBuilder {
		Item.ItemBuilder setName(String name);
		Item.ItemBuilder setPrice(BigDecimal price);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
			processor.processBasic(path.newSubPath("price"), BigDecimal.class, getPrice(), this);
		}
		

		Item.ItemBuilder prune();
	}

	/*********************** Immutable Implementation of Item  ***********************/
	class ItemImpl implements Item {
		private final String name;
		private final BigDecimal price;
		
		protected ItemImpl(Item.ItemBuilder builder) {
			this.name = builder.getName();
			this.price = builder.getPrice();
		}
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("price")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("price")
		public BigDecimal getPrice() {
			return price;
		}
		
		@Override
		public Item build() {
			return this;
		}
		
		@Override
		public Item.ItemBuilder toBuilder() {
			Item.ItemBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Item.ItemBuilder builder) {
			ofNullable(getName()).ifPresent(builder::setName);
			ofNullable(getPrice()).ifPresent(builder::setPrice);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Item _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(price, _that.getPrice())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (price != null ? price.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Item {" +
				"name=" + this.name + ", " +
				"price=" + this.price +
			'}';
		}
	}

	/*********************** Builder Implementation of Item  ***********************/
	class ItemBuilderImpl implements Item.ItemBuilder {
	
		protected String name;
		protected BigDecimal price;
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("price")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("price")
		public BigDecimal getPrice() {
			return price;
		}
		
		@RosettaAttribute("name")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("name")
		@Override
		public Item.ItemBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
			return this;
		}
		
		@RosettaAttribute("price")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("price")
		@Override
		public Item.ItemBuilder setPrice(BigDecimal _price) {
			this.price = _price == null ? null : _price;
			return this;
		}
		
		@Override
		public Item build() {
			return new Item.ItemImpl(this);
		}
		
		@Override
		public Item.ItemBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Item.ItemBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getName()!=null) return true;
			if (getPrice()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Item.ItemBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Item.ItemBuilder o = (Item.ItemBuilder) other;
			
			
			merger.mergeBasic(getName(), o.getName(), this::setName);
			merger.mergeBasic(getPrice(), o.getPrice(), this::setPrice);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Item _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(price, _that.getPrice())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (price != null ? price.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ItemBuilder {" +
				"name=" + this.name + ", " +
				"price=" + this.price +
			'}';
		}
	}
}
