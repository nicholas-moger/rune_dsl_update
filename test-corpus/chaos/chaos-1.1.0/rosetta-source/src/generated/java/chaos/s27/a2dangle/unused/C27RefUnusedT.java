package chaos.s27.a2dangle.unused;

import chaos.s27.a2dangle.unused.meta.C27RefUnusedTMeta;
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
@RosettaDataType(value="C27RefUnusedT", builder=C27RefUnusedT.C27RefUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C27RefUnusedT", model="chaos", builder=C27RefUnusedT.C27RefUnusedTBuilderImpl.class, version="1.0.0")
public interface C27RefUnusedT extends RosettaModelObject {

	C27RefUnusedTMeta metaData = new C27RefUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C27RefUnusedT build();
	
	C27RefUnusedT.C27RefUnusedTBuilder toBuilder();
	
	static C27RefUnusedT.C27RefUnusedTBuilder builder() {
		return new C27RefUnusedT.C27RefUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C27RefUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C27RefUnusedT> getType() {
		return C27RefUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C27RefUnusedTBuilder extends C27RefUnusedT, RosettaModelObjectBuilder {
		C27RefUnusedT.C27RefUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C27RefUnusedT.C27RefUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C27RefUnusedT  ***********************/
	class C27RefUnusedTImpl implements C27RefUnusedT {
		private final String stub;
		
		protected C27RefUnusedTImpl(C27RefUnusedT.C27RefUnusedTBuilder builder) {
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
		public C27RefUnusedT build() {
			return this;
		}
		
		@Override
		public C27RefUnusedT.C27RefUnusedTBuilder toBuilder() {
			C27RefUnusedT.C27RefUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C27RefUnusedT.C27RefUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C27RefUnusedT _that = getType().cast(o);
		
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
			return "C27RefUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C27RefUnusedT  ***********************/
	class C27RefUnusedTBuilderImpl implements C27RefUnusedT.C27RefUnusedTBuilder {
	
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
		public C27RefUnusedT.C27RefUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C27RefUnusedT build() {
			return new C27RefUnusedT.C27RefUnusedTImpl(this);
		}
		
		@Override
		public C27RefUnusedT.C27RefUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C27RefUnusedT.C27RefUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C27RefUnusedT.C27RefUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C27RefUnusedT.C27RefUnusedTBuilder o = (C27RefUnusedT.C27RefUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C27RefUnusedT _that = getType().cast(o);
		
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
			return "C27RefUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
