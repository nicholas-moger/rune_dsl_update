package chaos.s09.a2dangle.unused;

import chaos.s09.a2dangle.unused.meta.C9PlainUnusedTMeta;
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
@RosettaDataType(value="C9PlainUnusedT", builder=C9PlainUnusedT.C9PlainUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C9PlainUnusedT", model="chaos", builder=C9PlainUnusedT.C9PlainUnusedTBuilderImpl.class, version="1.0.0")
public interface C9PlainUnusedT extends RosettaModelObject {

	C9PlainUnusedTMeta metaData = new C9PlainUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C9PlainUnusedT build();
	
	C9PlainUnusedT.C9PlainUnusedTBuilder toBuilder();
	
	static C9PlainUnusedT.C9PlainUnusedTBuilder builder() {
		return new C9PlainUnusedT.C9PlainUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C9PlainUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C9PlainUnusedT> getType() {
		return C9PlainUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C9PlainUnusedTBuilder extends C9PlainUnusedT, RosettaModelObjectBuilder {
		C9PlainUnusedT.C9PlainUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C9PlainUnusedT.C9PlainUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C9PlainUnusedT  ***********************/
	class C9PlainUnusedTImpl implements C9PlainUnusedT {
		private final String stub;
		
		protected C9PlainUnusedTImpl(C9PlainUnusedT.C9PlainUnusedTBuilder builder) {
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
		public C9PlainUnusedT build() {
			return this;
		}
		
		@Override
		public C9PlainUnusedT.C9PlainUnusedTBuilder toBuilder() {
			C9PlainUnusedT.C9PlainUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C9PlainUnusedT.C9PlainUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C9PlainUnusedT _that = getType().cast(o);
		
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
			return "C9PlainUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C9PlainUnusedT  ***********************/
	class C9PlainUnusedTBuilderImpl implements C9PlainUnusedT.C9PlainUnusedTBuilder {
	
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
		public C9PlainUnusedT.C9PlainUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C9PlainUnusedT build() {
			return new C9PlainUnusedT.C9PlainUnusedTImpl(this);
		}
		
		@Override
		public C9PlainUnusedT.C9PlainUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C9PlainUnusedT.C9PlainUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C9PlainUnusedT.C9PlainUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C9PlainUnusedT.C9PlainUnusedTBuilder o = (C9PlainUnusedT.C9PlainUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C9PlainUnusedT _that = getType().cast(o);
		
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
			return "C9PlainUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
