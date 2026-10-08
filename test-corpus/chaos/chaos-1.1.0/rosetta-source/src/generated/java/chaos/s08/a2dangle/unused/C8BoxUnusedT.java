package chaos.s08.a2dangle.unused;

import chaos.s08.a2dangle.unused.meta.C8BoxUnusedTMeta;
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
@RosettaDataType(value="C8BoxUnusedT", builder=C8BoxUnusedT.C8BoxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C8BoxUnusedT", model="chaos", builder=C8BoxUnusedT.C8BoxUnusedTBuilderImpl.class, version="1.0.0")
public interface C8BoxUnusedT extends RosettaModelObject {

	C8BoxUnusedTMeta metaData = new C8BoxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C8BoxUnusedT build();
	
	C8BoxUnusedT.C8BoxUnusedTBuilder toBuilder();
	
	static C8BoxUnusedT.C8BoxUnusedTBuilder builder() {
		return new C8BoxUnusedT.C8BoxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C8BoxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C8BoxUnusedT> getType() {
		return C8BoxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C8BoxUnusedTBuilder extends C8BoxUnusedT, RosettaModelObjectBuilder {
		C8BoxUnusedT.C8BoxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C8BoxUnusedT.C8BoxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C8BoxUnusedT  ***********************/
	class C8BoxUnusedTImpl implements C8BoxUnusedT {
		private final String stub;
		
		protected C8BoxUnusedTImpl(C8BoxUnusedT.C8BoxUnusedTBuilder builder) {
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
		public C8BoxUnusedT build() {
			return this;
		}
		
		@Override
		public C8BoxUnusedT.C8BoxUnusedTBuilder toBuilder() {
			C8BoxUnusedT.C8BoxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C8BoxUnusedT.C8BoxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C8BoxUnusedT _that = getType().cast(o);
		
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
			return "C8BoxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C8BoxUnusedT  ***********************/
	class C8BoxUnusedTBuilderImpl implements C8BoxUnusedT.C8BoxUnusedTBuilder {
	
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
		public C8BoxUnusedT.C8BoxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C8BoxUnusedT build() {
			return new C8BoxUnusedT.C8BoxUnusedTImpl(this);
		}
		
		@Override
		public C8BoxUnusedT.C8BoxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C8BoxUnusedT.C8BoxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C8BoxUnusedT.C8BoxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C8BoxUnusedT.C8BoxUnusedTBuilder o = (C8BoxUnusedT.C8BoxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C8BoxUnusedT _that = getType().cast(o);
		
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
			return "C8BoxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
