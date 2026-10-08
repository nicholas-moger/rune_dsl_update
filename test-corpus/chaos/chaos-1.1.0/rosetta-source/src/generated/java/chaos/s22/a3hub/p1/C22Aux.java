package chaos.s22.a3hub.p1;

import chaos.s22.a3hub.p1.meta.C22AuxMeta;
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
@RosettaDataType(value="C22Aux", builder=C22Aux.C22AuxBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C22Aux", model="chaos", builder=C22Aux.C22AuxBuilderImpl.class, version="1.0.0")
public interface C22Aux extends RosettaModelObject {

	C22AuxMeta metaData = new C22AuxMeta();

	/*********************** Getter Methods  ***********************/
	String getAx();

	/*********************** Build Methods  ***********************/
	C22Aux build();
	
	C22Aux.C22AuxBuilder toBuilder();
	
	static C22Aux.C22AuxBuilder builder() {
		return new C22Aux.C22AuxBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C22Aux> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C22Aux> getType() {
		return C22Aux.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("ax"), String.class, getAx(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C22AuxBuilder extends C22Aux, RosettaModelObjectBuilder {
		C22Aux.C22AuxBuilder setAx(String ax);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("ax"), String.class, getAx(), this);
		}
		

		C22Aux.C22AuxBuilder prune();
	}

	/*********************** Immutable Implementation of C22Aux  ***********************/
	class C22AuxImpl implements C22Aux {
		private final String ax;
		
		protected C22AuxImpl(C22Aux.C22AuxBuilder builder) {
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
		public C22Aux build() {
			return this;
		}
		
		@Override
		public C22Aux.C22AuxBuilder toBuilder() {
			C22Aux.C22AuxBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C22Aux.C22AuxBuilder builder) {
			ofNullable(getAx()).ifPresent(builder::setAx);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C22Aux _that = getType().cast(o);
		
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
			return "C22Aux {" +
				"ax=" + this.ax +
			'}';
		}
	}

	/*********************** Builder Implementation of C22Aux  ***********************/
	class C22AuxBuilderImpl implements C22Aux.C22AuxBuilder {
	
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
		public C22Aux.C22AuxBuilder setAx(String _ax) {
			this.ax = _ax == null ? null : _ax;
			return this;
		}
		
		@Override
		public C22Aux build() {
			return new C22Aux.C22AuxImpl(this);
		}
		
		@Override
		public C22Aux.C22AuxBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C22Aux.C22AuxBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAx()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C22Aux.C22AuxBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C22Aux.C22AuxBuilder o = (C22Aux.C22AuxBuilder) other;
			
			
			merger.mergeBasic(getAx(), o.getAx(), this::setAx);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C22Aux _that = getType().cast(o);
		
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
			return "C22AuxBuilder {" +
				"ax=" + this.ax +
			'}';
		}
	}
}
