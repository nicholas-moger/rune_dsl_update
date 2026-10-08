package chaos.s34.a5uni;

import chaos.s34.a5uni.meta.C34AuxMeta;
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
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * éüµ – Helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C34Aux", builder=C34Aux.C34AuxBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C34Aux", model="chaos", builder=C34Aux.C34AuxBuilderImpl.class, version="1.0.0")
public interface C34Aux extends RosettaModelObject {

	C34AuxMeta metaData = new C34AuxMeta();

	/*********************** Getter Methods  ***********************/
	String getAux();

	/*********************** Build Methods  ***********************/
	C34Aux build();
	
	C34Aux.C34AuxBuilder toBuilder();
	
	static C34Aux.C34AuxBuilder builder() {
		return new C34Aux.C34AuxBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C34Aux> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C34Aux> getType() {
		return C34Aux.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("aux"), String.class, getAux(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C34AuxBuilder extends C34Aux, RosettaModelObjectBuilder {
		C34Aux.C34AuxBuilder setAux(String aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("aux"), String.class, getAux(), this);
		}
		

		C34Aux.C34AuxBuilder prune();
	}

	/*********************** Immutable Implementation of C34Aux  ***********************/
	class C34AuxImpl implements C34Aux {
		private final String aux;
		
		protected C34AuxImpl(C34Aux.C34AuxBuilder builder) {
			this.aux = builder.getAux();
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("aux")
		public String getAux() {
			return aux;
		}
		
		@Override
		public C34Aux build() {
			return this;
		}
		
		@Override
		public C34Aux.C34AuxBuilder toBuilder() {
			C34Aux.C34AuxBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C34Aux.C34AuxBuilder builder) {
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C34Aux _that = getType().cast(o);
		
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C34Aux {" +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C34Aux  ***********************/
	class C34AuxBuilderImpl implements C34Aux.C34AuxBuilder {
	
		protected String aux;
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("aux")
		public String getAux() {
			return aux;
		}
		
		@RosettaAttribute("aux")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("aux")
		@Override
		public C34Aux.C34AuxBuilder setAux(String _aux) {
			this.aux = _aux == null ? null : _aux;
			return this;
		}
		
		@Override
		public C34Aux build() {
			return new C34Aux.C34AuxImpl(this);
		}
		
		@Override
		public C34Aux.C34AuxBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C34Aux.C34AuxBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAux()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C34Aux.C34AuxBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C34Aux.C34AuxBuilder o = (C34Aux.C34AuxBuilder) other;
			
			
			merger.mergeBasic(getAux(), o.getAux(), this::setAux);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C34Aux _that = getType().cast(o);
		
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C34AuxBuilder {" +
				"aux=" + this.aux +
			'}';
		}
	}
}
