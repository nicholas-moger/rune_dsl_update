package chaos.s16.a2dangle.unused;

import chaos.s16.a2dangle.unused.meta.C16HeldUnusedTMeta;
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
@RosettaDataType(value="C16HeldUnusedT", builder=C16HeldUnusedT.C16HeldUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C16HeldUnusedT", model="chaos", builder=C16HeldUnusedT.C16HeldUnusedTBuilderImpl.class, version="1.0.0")
public interface C16HeldUnusedT extends RosettaModelObject {

	C16HeldUnusedTMeta metaData = new C16HeldUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C16HeldUnusedT build();
	
	C16HeldUnusedT.C16HeldUnusedTBuilder toBuilder();
	
	static C16HeldUnusedT.C16HeldUnusedTBuilder builder() {
		return new C16HeldUnusedT.C16HeldUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C16HeldUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C16HeldUnusedT> getType() {
		return C16HeldUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C16HeldUnusedTBuilder extends C16HeldUnusedT, RosettaModelObjectBuilder {
		C16HeldUnusedT.C16HeldUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C16HeldUnusedT.C16HeldUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C16HeldUnusedT  ***********************/
	class C16HeldUnusedTImpl implements C16HeldUnusedT {
		private final String stub;
		
		protected C16HeldUnusedTImpl(C16HeldUnusedT.C16HeldUnusedTBuilder builder) {
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
		public C16HeldUnusedT build() {
			return this;
		}
		
		@Override
		public C16HeldUnusedT.C16HeldUnusedTBuilder toBuilder() {
			C16HeldUnusedT.C16HeldUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C16HeldUnusedT.C16HeldUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16HeldUnusedT _that = getType().cast(o);
		
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
			return "C16HeldUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C16HeldUnusedT  ***********************/
	class C16HeldUnusedTBuilderImpl implements C16HeldUnusedT.C16HeldUnusedTBuilder {
	
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
		public C16HeldUnusedT.C16HeldUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C16HeldUnusedT build() {
			return new C16HeldUnusedT.C16HeldUnusedTImpl(this);
		}
		
		@Override
		public C16HeldUnusedT.C16HeldUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16HeldUnusedT.C16HeldUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16HeldUnusedT.C16HeldUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C16HeldUnusedT.C16HeldUnusedTBuilder o = (C16HeldUnusedT.C16HeldUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16HeldUnusedT _that = getType().cast(o);
		
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
			return "C16HeldUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
