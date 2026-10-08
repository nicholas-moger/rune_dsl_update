package chaos.s29.a2dangle.unused;

import chaos.s29.a2dangle.unused.meta.C29LeafUnusedTMeta;
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
@RosettaDataType(value="C29LeafUnusedT", builder=C29LeafUnusedT.C29LeafUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C29LeafUnusedT", model="chaos", builder=C29LeafUnusedT.C29LeafUnusedTBuilderImpl.class, version="1.0.0")
public interface C29LeafUnusedT extends RosettaModelObject {

	C29LeafUnusedTMeta metaData = new C29LeafUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C29LeafUnusedT build();
	
	C29LeafUnusedT.C29LeafUnusedTBuilder toBuilder();
	
	static C29LeafUnusedT.C29LeafUnusedTBuilder builder() {
		return new C29LeafUnusedT.C29LeafUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C29LeafUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C29LeafUnusedT> getType() {
		return C29LeafUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C29LeafUnusedTBuilder extends C29LeafUnusedT, RosettaModelObjectBuilder {
		C29LeafUnusedT.C29LeafUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C29LeafUnusedT.C29LeafUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C29LeafUnusedT  ***********************/
	class C29LeafUnusedTImpl implements C29LeafUnusedT {
		private final String stub;
		
		protected C29LeafUnusedTImpl(C29LeafUnusedT.C29LeafUnusedTBuilder builder) {
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
		public C29LeafUnusedT build() {
			return this;
		}
		
		@Override
		public C29LeafUnusedT.C29LeafUnusedTBuilder toBuilder() {
			C29LeafUnusedT.C29LeafUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C29LeafUnusedT.C29LeafUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29LeafUnusedT _that = getType().cast(o);
		
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
			return "C29LeafUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C29LeafUnusedT  ***********************/
	class C29LeafUnusedTBuilderImpl implements C29LeafUnusedT.C29LeafUnusedTBuilder {
	
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
		public C29LeafUnusedT.C29LeafUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C29LeafUnusedT build() {
			return new C29LeafUnusedT.C29LeafUnusedTImpl(this);
		}
		
		@Override
		public C29LeafUnusedT.C29LeafUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29LeafUnusedT.C29LeafUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29LeafUnusedT.C29LeafUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C29LeafUnusedT.C29LeafUnusedTBuilder o = (C29LeafUnusedT.C29LeafUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29LeafUnusedT _that = getType().cast(o);
		
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
			return "C29LeafUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
