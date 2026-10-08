package chaos.s05.a1o1;

import chaos.s05.a1o1.meta.C5SubMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneMetaType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.MetaFields;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * List-of-list rung and reference target - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C5Sub", builder=C5Sub.C5SubBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C5Sub", model="chaos", builder=C5Sub.C5SubBuilderImpl.class, version="1.0.0")
public interface C5Sub extends RosettaModelObject, GlobalKey {

	C5SubMeta metaData = new C5SubMeta();

	/*********************** Getter Methods  ***********************/
	List<BigDecimal> getVals();
	String getName();
	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	C5Sub build();
	
	C5Sub.C5SubBuilder toBuilder();
	
	static C5Sub.C5SubBuilder builder() {
		return new C5Sub.C5SubBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C5Sub> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C5Sub> getType() {
		return C5Sub.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("vals"), BigDecimal.class, getVals(), this);
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C5SubBuilder extends C5Sub, RosettaModelObjectBuilder, GlobalKey.GlobalKeyBuilder {
		MetaFields.MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFields.MetaFieldsBuilder getMeta();
		C5Sub.C5SubBuilder addVals(BigDecimal vals);
		C5Sub.C5SubBuilder addVals(BigDecimal vals, int idx);
		C5Sub.C5SubBuilder addVals(List<BigDecimal> vals);
		C5Sub.C5SubBuilder setVals(List<BigDecimal> vals);
		C5Sub.C5SubBuilder setName(String name);
		C5Sub.C5SubBuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("vals"), BigDecimal.class, getVals(), this);
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
			processRosetta(path.newSubPath("meta"), processor, MetaFields.MetaFieldsBuilder.class, getMeta());
		}
		

		C5Sub.C5SubBuilder prune();
	}

	/*********************** Immutable Implementation of C5Sub  ***********************/
	class C5SubImpl implements C5Sub {
		private final List<BigDecimal> vals;
		private final String name;
		private final MetaFields meta;
		
		protected C5SubImpl(C5Sub.C5SubBuilder builder) {
			this.vals = ofNullable(builder.getVals()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.name = builder.getName();
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("vals")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("vals")
		public List<BigDecimal> getVals() {
			return vals;
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
		@RosettaAttribute("meta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields getMeta() {
			return meta;
		}
		
		@Override
		public C5Sub build() {
			return this;
		}
		
		@Override
		public C5Sub.C5SubBuilder toBuilder() {
			C5Sub.C5SubBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C5Sub.C5SubBuilder builder) {
			ofNullable(getVals()).ifPresent(builder::setVals);
			ofNullable(getName()).ifPresent(builder::setName);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C5Sub _that = getType().cast(o);
		
			if (!ListEquals.listEquals(vals, _that.getVals())) return false;
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (vals != null ? vals.hashCode() : 0);
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C5Sub {" +
				"vals=" + this.vals + ", " +
				"name=" + this.name + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of C5Sub  ***********************/
	class C5SubBuilderImpl implements C5Sub.C5SubBuilder {
	
		protected List<BigDecimal> vals = new ArrayList<>();
		protected String name;
		protected MetaFields.MetaFieldsBuilder meta;
		
		@Override
		@RosettaAttribute("vals")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("vals")
		public List<BigDecimal> getVals() {
			return vals;
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
		@RosettaAttribute("meta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields.MetaFieldsBuilder getMeta() {
			return meta;
		}
		
		@Override
		public MetaFields.MetaFieldsBuilder getOrCreateMeta() {
			MetaFields.MetaFieldsBuilder result;
			if (meta!=null) {
				result = meta;
			}
			else {
				result = meta = MetaFields.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("vals")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("vals")
		@Override
		public C5Sub.C5SubBuilder addVals(BigDecimal _vals) {
			if (_vals != null) {
				this.vals.add(_vals);
			}
			return this;
		}
		
		@Override
		public C5Sub.C5SubBuilder addVals(BigDecimal _vals, int idx) {
			getIndex(this.vals, idx, () -> _vals);
			return this;
		}
		
		@Override
		public C5Sub.C5SubBuilder addVals(List<BigDecimal> valss) {
			if (valss != null) {
				for (final BigDecimal toAdd : valss) {
					this.vals.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("vals")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("vals")
		@Override
		public C5Sub.C5SubBuilder setVals(List<BigDecimal> valss) {
			if (valss == null) {
				this.vals = new ArrayList<>();
			} else {
				this.vals = valss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("name")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("name")
		@Override
		public C5Sub.C5SubBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
			return this;
		}
		
		@RosettaAttribute("meta")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		@Override
		public C5Sub.C5SubBuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public C5Sub build() {
			return new C5Sub.C5SubImpl(this);
		}
		
		@Override
		public C5Sub.C5SubBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C5Sub.C5SubBuilder prune() {
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getVals()!=null && !getVals().isEmpty()) return true;
			if (getName()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C5Sub.C5SubBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C5Sub.C5SubBuilder o = (C5Sub.C5SubBuilder) other;
			
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			merger.mergeBasic(getVals(), o.getVals(), (Consumer<BigDecimal>) this::addVals);
			merger.mergeBasic(getName(), o.getName(), this::setName);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C5Sub _that = getType().cast(o);
		
			if (!ListEquals.listEquals(vals, _that.getVals())) return false;
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (vals != null ? vals.hashCode() : 0);
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C5SubBuilder {" +
				"vals=" + this.vals + ", " +
				"name=" + this.name + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}
