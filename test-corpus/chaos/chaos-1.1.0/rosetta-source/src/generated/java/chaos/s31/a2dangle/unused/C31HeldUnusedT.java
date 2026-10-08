package chaos.s31.a2dangle.unused;

import chaos.s31.a2dangle.unused.meta.C31HeldUnusedTMeta;
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
@RosettaDataType(value="C31HeldUnusedT", builder=C31HeldUnusedT.C31HeldUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C31HeldUnusedT", model="chaos", builder=C31HeldUnusedT.C31HeldUnusedTBuilderImpl.class, version="1.0.0")
public interface C31HeldUnusedT extends RosettaModelObject {

	C31HeldUnusedTMeta metaData = new C31HeldUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C31HeldUnusedT build();
	
	C31HeldUnusedT.C31HeldUnusedTBuilder toBuilder();
	
	static C31HeldUnusedT.C31HeldUnusedTBuilder builder() {
		return new C31HeldUnusedT.C31HeldUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C31HeldUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C31HeldUnusedT> getType() {
		return C31HeldUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C31HeldUnusedTBuilder extends C31HeldUnusedT, RosettaModelObjectBuilder {
		C31HeldUnusedT.C31HeldUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C31HeldUnusedT.C31HeldUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C31HeldUnusedT  ***********************/
	class C31HeldUnusedTImpl implements C31HeldUnusedT {
		private final String stub;
		
		protected C31HeldUnusedTImpl(C31HeldUnusedT.C31HeldUnusedTBuilder builder) {
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
		public C31HeldUnusedT build() {
			return this;
		}
		
		@Override
		public C31HeldUnusedT.C31HeldUnusedTBuilder toBuilder() {
			C31HeldUnusedT.C31HeldUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C31HeldUnusedT.C31HeldUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C31HeldUnusedT _that = getType().cast(o);
		
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
			return "C31HeldUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C31HeldUnusedT  ***********************/
	class C31HeldUnusedTBuilderImpl implements C31HeldUnusedT.C31HeldUnusedTBuilder {
	
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
		public C31HeldUnusedT.C31HeldUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C31HeldUnusedT build() {
			return new C31HeldUnusedT.C31HeldUnusedTImpl(this);
		}
		
		@Override
		public C31HeldUnusedT.C31HeldUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C31HeldUnusedT.C31HeldUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C31HeldUnusedT.C31HeldUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C31HeldUnusedT.C31HeldUnusedTBuilder o = (C31HeldUnusedT.C31HeldUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C31HeldUnusedT _that = getType().cast(o);
		
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
			return "C31HeldUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
