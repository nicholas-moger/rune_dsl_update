package chaos.s21.a2dangle.unused;

import chaos.s21.a2dangle.unused.meta.C21AuxUnusedTMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
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
 * @version 1.0.0
 */
@RosettaDataType(value="C21AuxUnusedT", builder=C21AuxUnusedT.C21AuxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C21AuxUnusedT", model="chaos", builder=C21AuxUnusedT.C21AuxUnusedTBuilderImpl.class, version="1.0.0")
public interface C21AuxUnusedT extends RosettaModelObject {

	C21AuxUnusedTMeta metaData = new C21AuxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C21AuxUnusedT build();
	
	C21AuxUnusedT.C21AuxUnusedTBuilder toBuilder();
	
	static C21AuxUnusedT.C21AuxUnusedTBuilder builder() {
		return new C21AuxUnusedT.C21AuxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C21AuxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C21AuxUnusedT> getType() {
		return C21AuxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C21AuxUnusedTBuilder extends C21AuxUnusedT, RosettaModelObjectBuilder {
		C21AuxUnusedT.C21AuxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C21AuxUnusedT.C21AuxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C21AuxUnusedT  ***********************/
	class C21AuxUnusedTImpl implements C21AuxUnusedT {
		private final String stub;
		
		protected C21AuxUnusedTImpl(C21AuxUnusedT.C21AuxUnusedTBuilder builder) {
			this.stub = builder.getStub();
		}
		
		@Override
		@RosettaAttribute("stub")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("stub")
		public String getStub() {
			return stub;
		}
		
		@Override
		public C21AuxUnusedT build() {
			return this;
		}
		
		@Override
		public C21AuxUnusedT.C21AuxUnusedTBuilder toBuilder() {
			C21AuxUnusedT.C21AuxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C21AuxUnusedT.C21AuxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C21AuxUnusedT _that = getType().cast(o);
		
			if (!Objects.equals(stub, _that.getStub())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (stub != null ? stub.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C21AuxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C21AuxUnusedT  ***********************/
	class C21AuxUnusedTBuilderImpl implements C21AuxUnusedT.C21AuxUnusedTBuilder {
	
		protected String stub;
		
		@Override
		@RosettaAttribute("stub")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("stub")
		public String getStub() {
			return stub;
		}
		
		@RosettaAttribute("stub")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("stub")
		@Override
		public C21AuxUnusedT.C21AuxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C21AuxUnusedT build() {
			return new C21AuxUnusedT.C21AuxUnusedTImpl(this);
		}
		
		@Override
		public C21AuxUnusedT.C21AuxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C21AuxUnusedT.C21AuxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C21AuxUnusedT.C21AuxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C21AuxUnusedT.C21AuxUnusedTBuilder o = (C21AuxUnusedT.C21AuxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C21AuxUnusedT _that = getType().cast(o);
		
			if (!Objects.equals(stub, _that.getStub())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (stub != null ? stub.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C21AuxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
