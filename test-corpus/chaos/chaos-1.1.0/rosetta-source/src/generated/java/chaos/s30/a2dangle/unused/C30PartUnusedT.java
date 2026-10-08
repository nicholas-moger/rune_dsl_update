package chaos.s30.a2dangle.unused;

import chaos.s30.a2dangle.unused.meta.C30PartUnusedTMeta;
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
@RosettaDataType(value="C30PartUnusedT", builder=C30PartUnusedT.C30PartUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C30PartUnusedT", model="chaos", builder=C30PartUnusedT.C30PartUnusedTBuilderImpl.class, version="1.0.0")
public interface C30PartUnusedT extends RosettaModelObject {

	C30PartUnusedTMeta metaData = new C30PartUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C30PartUnusedT build();
	
	C30PartUnusedT.C30PartUnusedTBuilder toBuilder();
	
	static C30PartUnusedT.C30PartUnusedTBuilder builder() {
		return new C30PartUnusedT.C30PartUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C30PartUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C30PartUnusedT> getType() {
		return C30PartUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C30PartUnusedTBuilder extends C30PartUnusedT, RosettaModelObjectBuilder {
		C30PartUnusedT.C30PartUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C30PartUnusedT.C30PartUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C30PartUnusedT  ***********************/
	class C30PartUnusedTImpl implements C30PartUnusedT {
		private final String stub;
		
		protected C30PartUnusedTImpl(C30PartUnusedT.C30PartUnusedTBuilder builder) {
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
		public C30PartUnusedT build() {
			return this;
		}
		
		@Override
		public C30PartUnusedT.C30PartUnusedTBuilder toBuilder() {
			C30PartUnusedT.C30PartUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C30PartUnusedT.C30PartUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C30PartUnusedT _that = getType().cast(o);
		
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
			return "C30PartUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C30PartUnusedT  ***********************/
	class C30PartUnusedTBuilderImpl implements C30PartUnusedT.C30PartUnusedTBuilder {
	
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
		public C30PartUnusedT.C30PartUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C30PartUnusedT build() {
			return new C30PartUnusedT.C30PartUnusedTImpl(this);
		}
		
		@Override
		public C30PartUnusedT.C30PartUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C30PartUnusedT.C30PartUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C30PartUnusedT.C30PartUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C30PartUnusedT.C30PartUnusedTBuilder o = (C30PartUnusedT.C30PartUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C30PartUnusedT _that = getType().cast(o);
		
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
			return "C30PartUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
