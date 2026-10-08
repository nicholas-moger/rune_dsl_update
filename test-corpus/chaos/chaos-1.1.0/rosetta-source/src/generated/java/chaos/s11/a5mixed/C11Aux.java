package chaos.s11.a5mixed;

import chaos.s11.a5mixed.meta.C11AuxMeta;
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
@RosettaDataType(value="C11Aux", builder=C11Aux.C11AuxBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C11Aux", model="chaos", builder=C11Aux.C11AuxBuilderImpl.class, version="1.0.0")
public interface C11Aux extends RosettaModelObject {

	C11AuxMeta metaData = new C11AuxMeta();

	/*********************** Getter Methods  ***********************/
	String getAx();

	/*********************** Build Methods  ***********************/
	C11Aux build();
	
	C11Aux.C11AuxBuilder toBuilder();
	
	static C11Aux.C11AuxBuilder builder() {
		return new C11Aux.C11AuxBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C11Aux> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C11Aux> getType() {
		return C11Aux.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("ax"), String.class, getAx(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C11AuxBuilder extends C11Aux, RosettaModelObjectBuilder {
		C11Aux.C11AuxBuilder setAx(String ax);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("ax"), String.class, getAx(), this);
		}
		

		C11Aux.C11AuxBuilder prune();
	}

	/*********************** Immutable Implementation of C11Aux  ***********************/
	class C11AuxImpl implements C11Aux {
		private final String ax;
		
		protected C11AuxImpl(C11Aux.C11AuxBuilder builder) {
			this.ax = builder.getAx();
		}
		
		@Override
		@RosettaAttribute("ax")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("ax")
		public String getAx() {
			return ax;
		}
		
		@Override
		public C11Aux build() {
			return this;
		}
		
		@Override
		public C11Aux.C11AuxBuilder toBuilder() {
			C11Aux.C11AuxBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C11Aux.C11AuxBuilder builder) {
			ofNullable(getAx()).ifPresent(builder::setAx);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C11Aux _that = getType().cast(o);
		
			if (!Objects.equals(ax, _that.getAx())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ax != null ? ax.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C11Aux {" +
				"ax=" + this.ax +
			'}';
		}
	}

	/*********************** Builder Implementation of C11Aux  ***********************/
	class C11AuxBuilderImpl implements C11Aux.C11AuxBuilder {
	
		protected String ax;
		
		@Override
		@RosettaAttribute("ax")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("ax")
		public String getAx() {
			return ax;
		}
		
		@RosettaAttribute("ax")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("ax")
		@Override
		public C11Aux.C11AuxBuilder setAx(String _ax) {
			this.ax = _ax == null ? null : _ax;
			return this;
		}
		
		@Override
		public C11Aux build() {
			return new C11Aux.C11AuxImpl(this);
		}
		
		@Override
		public C11Aux.C11AuxBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C11Aux.C11AuxBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAx()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C11Aux.C11AuxBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C11Aux.C11AuxBuilder o = (C11Aux.C11AuxBuilder) other;
			
			
			merger.mergeBasic(getAx(), o.getAx(), this::setAx);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C11Aux _that = getType().cast(o);
		
			if (!Objects.equals(ax, _that.getAx())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ax != null ? ax.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C11AuxBuilder {" +
				"ax=" + this.ax +
			'}';
		}
	}
}
