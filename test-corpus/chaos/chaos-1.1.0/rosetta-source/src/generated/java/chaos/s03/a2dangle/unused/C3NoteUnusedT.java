package chaos.s03.a2dangle.unused;

import chaos.s03.a2dangle.unused.meta.C3NoteUnusedTMeta;
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
@RosettaDataType(value="C3NoteUnusedT", builder=C3NoteUnusedT.C3NoteUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C3NoteUnusedT", model="chaos", builder=C3NoteUnusedT.C3NoteUnusedTBuilderImpl.class, version="1.0.0")
public interface C3NoteUnusedT extends RosettaModelObject {

	C3NoteUnusedTMeta metaData = new C3NoteUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C3NoteUnusedT build();
	
	C3NoteUnusedT.C3NoteUnusedTBuilder toBuilder();
	
	static C3NoteUnusedT.C3NoteUnusedTBuilder builder() {
		return new C3NoteUnusedT.C3NoteUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C3NoteUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C3NoteUnusedT> getType() {
		return C3NoteUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C3NoteUnusedTBuilder extends C3NoteUnusedT, RosettaModelObjectBuilder {
		C3NoteUnusedT.C3NoteUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C3NoteUnusedT.C3NoteUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C3NoteUnusedT  ***********************/
	class C3NoteUnusedTImpl implements C3NoteUnusedT {
		private final String stub;
		
		protected C3NoteUnusedTImpl(C3NoteUnusedT.C3NoteUnusedTBuilder builder) {
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
		public C3NoteUnusedT build() {
			return this;
		}
		
		@Override
		public C3NoteUnusedT.C3NoteUnusedTBuilder toBuilder() {
			C3NoteUnusedT.C3NoteUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C3NoteUnusedT.C3NoteUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3NoteUnusedT _that = getType().cast(o);
		
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
			return "C3NoteUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C3NoteUnusedT  ***********************/
	class C3NoteUnusedTBuilderImpl implements C3NoteUnusedT.C3NoteUnusedTBuilder {
	
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
		public C3NoteUnusedT.C3NoteUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C3NoteUnusedT build() {
			return new C3NoteUnusedT.C3NoteUnusedTImpl(this);
		}
		
		@Override
		public C3NoteUnusedT.C3NoteUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3NoteUnusedT.C3NoteUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3NoteUnusedT.C3NoteUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C3NoteUnusedT.C3NoteUnusedTBuilder o = (C3NoteUnusedT.C3NoteUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3NoteUnusedT _that = getType().cast(o);
		
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
			return "C3NoteUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
