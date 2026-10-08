package chaos.s12.a2wild;

import chaos.s12.a2wild.h.C12Aux;
import chaos.s12.a2wild.meta.C12RowMeta;
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

import static java.util.Optional.ofNullable;

/**
 * Mapping target for the external source.
 * @version 1.0.0
 */
@RosettaDataType(value="C12Row", builder=C12Row.C12RowBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C12Row", model="chaos", builder=C12Row.C12RowBuilderImpl.class, version="1.0.0")
public interface C12Row extends RosettaModelObject {

	C12RowMeta metaData = new C12RowMeta();

	/*********************** Getter Methods  ***********************/
	String getKey();
	BigDecimal getVal();
	C12Aux getAux();

	/*********************** Build Methods  ***********************/
	C12Row build();
	
	C12Row.C12RowBuilder toBuilder();
	
	static C12Row.C12RowBuilder builder() {
		return new C12Row.C12RowBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C12Row> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C12Row> getType() {
		return C12Row.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("key"), String.class, getKey(), this);
		processor.processBasic(path.newSubPath("val"), BigDecimal.class, getVal(), this);
		processRosetta(path.newSubPath("aux"), processor, C12Aux.class, getAux());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C12RowBuilder extends C12Row, RosettaModelObjectBuilder {
		C12Aux.C12AuxBuilder getOrCreateAux();
		@Override
		C12Aux.C12AuxBuilder getAux();
		C12Row.C12RowBuilder setKey(String key);
		C12Row.C12RowBuilder setVal(BigDecimal val);
		C12Row.C12RowBuilder setAux(C12Aux aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("key"), String.class, getKey(), this);
			processor.processBasic(path.newSubPath("val"), BigDecimal.class, getVal(), this);
			processRosetta(path.newSubPath("aux"), processor, C12Aux.C12AuxBuilder.class, getAux());
		}
		

		C12Row.C12RowBuilder prune();
	}

	/*********************** Immutable Implementation of C12Row  ***********************/
	class C12RowImpl implements C12Row {
		private final String key;
		private final BigDecimal val;
		private final C12Aux aux;
		
		protected C12RowImpl(C12Row.C12RowBuilder builder) {
			this.key = builder.getKey();
			this.val = builder.getVal();
			this.aux = ofNullable(builder.getAux()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("key")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("key")
		public String getKey() {
			return key;
		}
		
		@Override
		@RosettaAttribute("val")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("val")
		public BigDecimal getVal() {
			return val;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C12Aux getAux() {
			return aux;
		}
		
		@Override
		public C12Row build() {
			return this;
		}
		
		@Override
		public C12Row.C12RowBuilder toBuilder() {
			C12Row.C12RowBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C12Row.C12RowBuilder builder) {
			ofNullable(getKey()).ifPresent(builder::setKey);
			ofNullable(getVal()).ifPresent(builder::setVal);
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C12Row _that = getType().cast(o);
		
			if (!Objects.equals(key, _that.getKey())) return false;
			if (!Objects.equals(val, _that.getVal())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (key != null ? key.hashCode() : 0);
			_result = 31 * _result + (val != null ? val.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C12Row {" +
				"key=" + this.key + ", " +
				"val=" + this.val + ", " +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C12Row  ***********************/
	class C12RowBuilderImpl implements C12Row.C12RowBuilder {
	
		protected String key;
		protected BigDecimal val;
		protected C12Aux.C12AuxBuilder aux;
		
		@Override
		@RosettaAttribute("key")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("key")
		public String getKey() {
			return key;
		}
		
		@Override
		@RosettaAttribute("val")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("val")
		public BigDecimal getVal() {
			return val;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C12Aux.C12AuxBuilder getAux() {
			return aux;
		}
		
		@Override
		public C12Aux.C12AuxBuilder getOrCreateAux() {
			C12Aux.C12AuxBuilder result;
			if (aux!=null) {
				result = aux;
			}
			else {
				result = aux = C12Aux.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("key")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("key")
		@Override
		public C12Row.C12RowBuilder setKey(String _key) {
			this.key = _key == null ? null : _key;
			return this;
		}
		
		@RosettaAttribute("val")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("val")
		@Override
		public C12Row.C12RowBuilder setVal(BigDecimal _val) {
			this.val = _val == null ? null : _val;
			return this;
		}
		
		@RosettaAttribute("aux")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("aux")
		@Override
		public C12Row.C12RowBuilder setAux(C12Aux _aux) {
			this.aux = _aux == null ? null : _aux.toBuilder();
			return this;
		}
		
		@Override
		public C12Row build() {
			return new C12Row.C12RowImpl(this);
		}
		
		@Override
		public C12Row.C12RowBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C12Row.C12RowBuilder prune() {
			if (aux!=null && !aux.prune().hasData()) aux = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getKey()!=null) return true;
			if (getVal()!=null) return true;
			if (getAux()!=null && getAux().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C12Row.C12RowBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C12Row.C12RowBuilder o = (C12Row.C12RowBuilder) other;
			
			merger.mergeRosetta(getAux(), o.getAux(), this::setAux);
			
			merger.mergeBasic(getKey(), o.getKey(), this::setKey);
			merger.mergeBasic(getVal(), o.getVal(), this::setVal);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C12Row _that = getType().cast(o);
		
			if (!Objects.equals(key, _that.getKey())) return false;
			if (!Objects.equals(val, _that.getVal())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (key != null ? key.hashCode() : 0);
			_result = 31 * _result + (val != null ? val.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C12RowBuilder {" +
				"key=" + this.key + ", " +
				"val=" + this.val + ", " +
				"aux=" + this.aux +
			'}';
		}
	}
}
