package chaos.s10.a5mixed;

import chaos.s10.a5mixed.meta.C10AuxMeta;
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
 * Self-contained helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C10Aux", builder=C10Aux.C10AuxBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C10Aux", model="chaos", builder=C10Aux.C10AuxBuilderImpl.class, version="1.0.0")
public interface C10Aux extends RosettaModelObject {

	C10AuxMeta metaData = new C10AuxMeta();

	/*********************** Getter Methods  ***********************/
	String getAux();

	/*********************** Build Methods  ***********************/
	C10Aux build();
	
	C10Aux.C10AuxBuilder toBuilder();
	
	static C10Aux.C10AuxBuilder builder() {
		return new C10Aux.C10AuxBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C10Aux> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C10Aux> getType() {
		return C10Aux.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("aux"), String.class, getAux(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C10AuxBuilder extends C10Aux, RosettaModelObjectBuilder {
		C10Aux.C10AuxBuilder setAux(String aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("aux"), String.class, getAux(), this);
		}
		

		C10Aux.C10AuxBuilder prune();
	}

	/*********************** Immutable Implementation of C10Aux  ***********************/
	class C10AuxImpl implements C10Aux {
		private final String aux;
		
		protected C10AuxImpl(C10Aux.C10AuxBuilder builder) {
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
		public C10Aux build() {
			return this;
		}
		
		@Override
		public C10Aux.C10AuxBuilder toBuilder() {
			C10Aux.C10AuxBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C10Aux.C10AuxBuilder builder) {
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C10Aux _that = getType().cast(o);
		
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
			return "C10Aux {" +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C10Aux  ***********************/
	class C10AuxBuilderImpl implements C10Aux.C10AuxBuilder {
	
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
		public C10Aux.C10AuxBuilder setAux(String _aux) {
			this.aux = _aux == null ? null : _aux;
			return this;
		}
		
		@Override
		public C10Aux build() {
			return new C10Aux.C10AuxImpl(this);
		}
		
		@Override
		public C10Aux.C10AuxBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C10Aux.C10AuxBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAux()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C10Aux.C10AuxBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C10Aux.C10AuxBuilder o = (C10Aux.C10AuxBuilder) other;
			
			
			merger.mergeBasic(getAux(), o.getAux(), this::setAux);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C10Aux _that = getType().cast(o);
		
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
			return "C10AuxBuilder {" +
				"aux=" + this.aux +
			'}';
		}
	}
}
