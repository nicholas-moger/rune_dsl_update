package chaos.s20.a2dangle.unused;

import chaos.s20.a2dangle.unused.meta.C20LeafUnusedTMeta;
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
@RosettaDataType(value="C20LeafUnusedT", builder=C20LeafUnusedT.C20LeafUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C20LeafUnusedT", model="chaos", builder=C20LeafUnusedT.C20LeafUnusedTBuilderImpl.class, version="1.0.0")
public interface C20LeafUnusedT extends RosettaModelObject {

	C20LeafUnusedTMeta metaData = new C20LeafUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C20LeafUnusedT build();
	
	C20LeafUnusedT.C20LeafUnusedTBuilder toBuilder();
	
	static C20LeafUnusedT.C20LeafUnusedTBuilder builder() {
		return new C20LeafUnusedT.C20LeafUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C20LeafUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C20LeafUnusedT> getType() {
		return C20LeafUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C20LeafUnusedTBuilder extends C20LeafUnusedT, RosettaModelObjectBuilder {
		C20LeafUnusedT.C20LeafUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C20LeafUnusedT.C20LeafUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C20LeafUnusedT  ***********************/
	class C20LeafUnusedTImpl implements C20LeafUnusedT {
		private final String stub;
		
		protected C20LeafUnusedTImpl(C20LeafUnusedT.C20LeafUnusedTBuilder builder) {
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
		public C20LeafUnusedT build() {
			return this;
		}
		
		@Override
		public C20LeafUnusedT.C20LeafUnusedTBuilder toBuilder() {
			C20LeafUnusedT.C20LeafUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C20LeafUnusedT.C20LeafUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C20LeafUnusedT _that = getType().cast(o);
		
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
			return "C20LeafUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C20LeafUnusedT  ***********************/
	class C20LeafUnusedTBuilderImpl implements C20LeafUnusedT.C20LeafUnusedTBuilder {
	
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
		public C20LeafUnusedT.C20LeafUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C20LeafUnusedT build() {
			return new C20LeafUnusedT.C20LeafUnusedTImpl(this);
		}
		
		@Override
		public C20LeafUnusedT.C20LeafUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C20LeafUnusedT.C20LeafUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C20LeafUnusedT.C20LeafUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C20LeafUnusedT.C20LeafUnusedTBuilder o = (C20LeafUnusedT.C20LeafUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C20LeafUnusedT _that = getType().cast(o);
		
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
			return "C20LeafUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
