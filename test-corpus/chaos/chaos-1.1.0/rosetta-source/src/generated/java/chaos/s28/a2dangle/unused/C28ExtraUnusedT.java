package chaos.s28.a2dangle.unused;

import chaos.s28.a2dangle.unused.meta.C28ExtraUnusedTMeta;
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
@RosettaDataType(value="C28ExtraUnusedT", builder=C28ExtraUnusedT.C28ExtraUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C28ExtraUnusedT", model="chaos", builder=C28ExtraUnusedT.C28ExtraUnusedTBuilderImpl.class, version="1.0.0")
public interface C28ExtraUnusedT extends RosettaModelObject {

	C28ExtraUnusedTMeta metaData = new C28ExtraUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C28ExtraUnusedT build();
	
	C28ExtraUnusedT.C28ExtraUnusedTBuilder toBuilder();
	
	static C28ExtraUnusedT.C28ExtraUnusedTBuilder builder() {
		return new C28ExtraUnusedT.C28ExtraUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C28ExtraUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C28ExtraUnusedT> getType() {
		return C28ExtraUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C28ExtraUnusedTBuilder extends C28ExtraUnusedT, RosettaModelObjectBuilder {
		C28ExtraUnusedT.C28ExtraUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C28ExtraUnusedT.C28ExtraUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C28ExtraUnusedT  ***********************/
	class C28ExtraUnusedTImpl implements C28ExtraUnusedT {
		private final String stub;
		
		protected C28ExtraUnusedTImpl(C28ExtraUnusedT.C28ExtraUnusedTBuilder builder) {
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
		public C28ExtraUnusedT build() {
			return this;
		}
		
		@Override
		public C28ExtraUnusedT.C28ExtraUnusedTBuilder toBuilder() {
			C28ExtraUnusedT.C28ExtraUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C28ExtraUnusedT.C28ExtraUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28ExtraUnusedT _that = getType().cast(o);
		
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
			return "C28ExtraUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C28ExtraUnusedT  ***********************/
	class C28ExtraUnusedTBuilderImpl implements C28ExtraUnusedT.C28ExtraUnusedTBuilder {
	
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
		public C28ExtraUnusedT.C28ExtraUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C28ExtraUnusedT build() {
			return new C28ExtraUnusedT.C28ExtraUnusedTImpl(this);
		}
		
		@Override
		public C28ExtraUnusedT.C28ExtraUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28ExtraUnusedT.C28ExtraUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28ExtraUnusedT.C28ExtraUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C28ExtraUnusedT.C28ExtraUnusedTBuilder o = (C28ExtraUnusedT.C28ExtraUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28ExtraUnusedT _that = getType().cast(o);
		
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
			return "C28ExtraUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
