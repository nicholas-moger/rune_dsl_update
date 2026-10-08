package chaos.s19.a2dangle.unused;

import chaos.s19.a2dangle.unused.meta.C19PartUnusedTMeta;
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
@RosettaDataType(value="C19PartUnusedT", builder=C19PartUnusedT.C19PartUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C19PartUnusedT", model="chaos", builder=C19PartUnusedT.C19PartUnusedTBuilderImpl.class, version="1.0.0")
public interface C19PartUnusedT extends RosettaModelObject {

	C19PartUnusedTMeta metaData = new C19PartUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C19PartUnusedT build();
	
	C19PartUnusedT.C19PartUnusedTBuilder toBuilder();
	
	static C19PartUnusedT.C19PartUnusedTBuilder builder() {
		return new C19PartUnusedT.C19PartUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C19PartUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C19PartUnusedT> getType() {
		return C19PartUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C19PartUnusedTBuilder extends C19PartUnusedT, RosettaModelObjectBuilder {
		C19PartUnusedT.C19PartUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C19PartUnusedT.C19PartUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C19PartUnusedT  ***********************/
	class C19PartUnusedTImpl implements C19PartUnusedT {
		private final String stub;
		
		protected C19PartUnusedTImpl(C19PartUnusedT.C19PartUnusedTBuilder builder) {
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
		public C19PartUnusedT build() {
			return this;
		}
		
		@Override
		public C19PartUnusedT.C19PartUnusedTBuilder toBuilder() {
			C19PartUnusedT.C19PartUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C19PartUnusedT.C19PartUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C19PartUnusedT _that = getType().cast(o);
		
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
			return "C19PartUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C19PartUnusedT  ***********************/
	class C19PartUnusedTBuilderImpl implements C19PartUnusedT.C19PartUnusedTBuilder {
	
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
		public C19PartUnusedT.C19PartUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C19PartUnusedT build() {
			return new C19PartUnusedT.C19PartUnusedTImpl(this);
		}
		
		@Override
		public C19PartUnusedT.C19PartUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C19PartUnusedT.C19PartUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C19PartUnusedT.C19PartUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C19PartUnusedT.C19PartUnusedTBuilder o = (C19PartUnusedT.C19PartUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C19PartUnusedT _that = getType().cast(o);
		
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
			return "C19PartUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
